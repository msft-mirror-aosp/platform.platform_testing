/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#![allow(missing_docs)]

//! SDV Test Service For Service Discovery

use binder::{binder_impl::Binder, Interface};

use futures_util::future::FutureExt as _;

use grpcio::{Environment, RpcContext, ServerBuilder, ServerCredentials, UnarySink};

use log::{error, info};

use std::env;
use std::io;
use std::io::Read;
use std::sync::{Arc, Mutex};

use tokio::sync::oneshot;

use google_sdv_identity::aidl::google::sdv::identity::IIdentityAgent::{BpIdentityAgent, IIdentityAgent};
use google_sdv_identity::aidl::google::sdv::identity::ServiceFqin::ServiceFqin;
use google_sdv_identity::aidl::google::sdv::identity::ServiceIdentity::EcPublicKey::EcPublicKey;
use google_sdv_service_discovery_common::aidl::google::sdv::service_discovery::common::metadata::ApplicationMetadata::ApplicationMetadata;
use google_sdv_service_discovery_common::aidl::google::sdv::service_discovery::common::metadata::ApplicationMetadata::TypedMetadata::TypedMetadata::ServerUnit;
use google_sdv_service_discovery_common::aidl::google::sdv::service_discovery::common::metadata::ServerUnitMetadata::ServerUnitMetadata;
use google_sdv_service_discovery_common::aidl::google::sdv::service_discovery::common::metadata::TransportMetadata::TransportMetadata;
use google_sdv_service_discovery_common::aidl::google::sdv::service_discovery::common::AccessControl::AccessControl;
use google_sdv_service_discovery_common::aidl::google::sdv::service_discovery::common::RegistrationToken::RegistrationToken;
use google_sdv_service_discovery_common::aidl::google::sdv::service_discovery::common::UnitType::UnitType;
use google_sdv_service_discovery_discovery::aidl::google::sdv::service_discovery::discovery::IServiceDiscoveryAgent::{BpServiceDiscoveryAgent, IServiceDiscoveryAgent};
use google_sdv_service_discovery_discovery::aidl::google::sdv::service_discovery::discovery::IServiceRegistrationAgent::IServiceRegistrationAgent;
use google_sdv_service_discovery_discovery::aidl::google::sdv::service_discovery::discovery::IServiceRegistrationAgent::BpServiceRegistrationAgent;
use google_sdv_service_discovery_discovery::aidl::google::sdv::service_discovery::discovery::ITransportSupportAgent::{ITransportSupportAgent, BpTransportSupportAgent};
use google_sdv_service_discovery_discovery::aidl::google::sdv::service_discovery::discovery::UnitNameParameters::UnitNameParameters;

use sdv_log::init_logger;

use service_discovery_test_service_proto::service_discovery::AddTransportMetadataRequest;
use service_discovery_test_service_proto::service_discovery::CreateIdentityRequest;
use service_discovery_test_service_proto::service_discovery::RegisterServiceUnitRequest;
use service_discovery_test_service_proto::service_discovery::ServiceDiscoveryResponse;
use service_discovery_test_service_proto::service_discovery::UnitNameRequest;
use service_discovery_test_service_proto::service_discovery::UnitTypeRequest;
use service_discovery_test_service_proto::service_discovery_grpc::{
    create_service_discovery, ServiceDiscovery,
};

static LOG_TAG: &str = "service_discovery_test_service";

#[derive(Clone)]
struct ServiceDiscoveryService {
    registered_unit_token: Arc<Mutex<Option<RegistrationToken>>>,
}

impl ServiceDiscoveryService {
    pub(crate) fn new() -> Self {
        ServiceDiscoveryService { registered_unit_token: Arc::new(Mutex::new(None)) }
    }
}

impl ServiceDiscovery for ServiceDiscoveryService {
    fn create_identity(
        &mut self,
        ctx: RpcContext,
        req: CreateIdentityRequest,
        sink: UnarySink<ServiceDiscoveryResponse>,
    ) {
        info!(":::: create_identity :::: Received Create Identity Request");
        let descriptor =
            <BpIdentityAgent as IIdentityAgent>::get_descriptor().to_owned() + "/default";
        let identity_agent: binder::Strong<dyn IIdentityAgent> =
            binder::get_interface(&descriptor).unwrap();

        let public_key_binding = req.public_key.to_string();
        let public_key = public_key_binding.as_bytes();
        let len = public_key.len();
        let mut public_key_array: [u8; 32] = Default::default();
        public_key_array[..len].clone_from_slice(public_key);

        let publickey = EcPublicKey { keyValue: public_key_array };

        let fqin = ServiceFqin {
            sdvVmName: req.service_fqin.clone().unwrap().sdvVmName.to_string(),
            sdvPackageName: req.service_fqin.clone().unwrap().sdvPackageName.to_string(),
            serviceBundleName: req.service_fqin.clone().unwrap().serviceBundleName.to_string(),
            serviceInstanceName: req.service_fqin.clone().unwrap().serviceInstanceName.to_string(),
        };

        let link_to_death = Binder::new(()).as_binder();

        let result = identity_agent.createIdentity(&publickey, &fqin, &link_to_death);

        info!(":::: create_identity :::: Create Identity Result: {result:?}");

        let mut response = ServiceDiscoveryResponse::new();
        response.response_message = format!("Create Identity Result: {result:?}");
        ctx.spawn(sink.success(response).map(|_| ()));
    }

    fn register_service_unit(
        &mut self,
        ctx: RpcContext,
        req: RegisterServiceUnitRequest,
        sink: UnarySink<ServiceDiscoveryResponse>,
    ) {
        info!(":::: register_service_unit :::: Received Register Service Unit Request");
        let descriptor =
            <BpServiceRegistrationAgent as IServiceRegistrationAgent>::get_descriptor().to_owned()
                + "/default";
        let registration_agent: binder::Strong<dyn IServiceRegistrationAgent> =
            binder::get_interface(&descriptor).unwrap();

        let unit_type = UnitType {
            sdvPackageName: req.unit_type.clone().unwrap().sdvPackageName.to_string(),
            serviceBundleName: req.unit_type.clone().unwrap().serviceBundleName.to_string(),
            typeName: req.unit_type.clone().unwrap().typeName.to_string(),
        };

        let acl: AccessControl = AccessControl { ..Default::default() };

        let app_metadata = ApplicationMetadata {
            typedMetadata: ServerUnit(ServerUnitMetadata {
                version: req.app_metadata.clone().unwrap().version,
            }),
            valueHolder: req.app_metadata.clone().unwrap().valueHolder.to_string().into(),
        };

        let result = registration_agent.registerServiceUnit(
            &req.service_unit_name.to_string(),
            &unit_type,
            &acl,
            &app_metadata,
        );

        info!(":::: register_service_unit :::: Register Service Unit Result: {result:?}");

        let mut response = ServiceDiscoveryResponse::new();
        response.response_message = format!("Register Service Unit Result: {result:?}");

        ctx.spawn(sink.success(response).map(|_| ()));

        if let Ok(result) = result {
            info!(":::: register_service_unit :::: Save Registered Token: {result:?}");
            self.registered_unit_token.lock().unwrap().replace(result);
        }

        if self.registered_unit_token.lock().unwrap().is_none() {
            error!(":::: register_service_unit :::: Failed To Save Registered Token");
        } else {
            info!(
                ":::: register_service_unit :::: Successfully Saved Registered Token: {:?}",
                self.registered_unit_token.lock().unwrap()
            );
        }
    }

    fn add_transport_metadata(
        &mut self,
        ctx: RpcContext,
        req: AddTransportMetadataRequest,
        sink: UnarySink<ServiceDiscoveryResponse>,
    ) {
        info!(":::: add_transport_metadata :::: Received Add Transport Metadata Request");
        if self.registered_unit_token.lock().unwrap().is_none() {
            error!(":::: add_transport_metadata :::: Service Unit Not Registered. Please Register Service Unit First Using RegisterServiceUnit API.");
            let mut response = ServiceDiscoveryResponse::new();
            response.response_message = "Service Unit Not Registered".to_string();
            ctx.spawn(sink.success(response).map(|_| ()));
            return;
        }

        let descriptor = <BpTransportSupportAgent as ITransportSupportAgent>::get_descriptor()
            .to_owned()
            + "/default";
        let transport_support_agent: binder::Strong<dyn ITransportSupportAgent> =
            binder::get_interface(&descriptor).unwrap();

        let transport_metadata =
            TransportMetadata { value_holder: req.value_holder.to_string().into() };

        let token_binding = self.registered_unit_token.lock().unwrap();
        let token = token_binding.as_ref().unwrap();

        let result = transport_support_agent.setAgentMetadata(token, &transport_metadata);
        info!(":::: add_transport_metadata :::: Add Transport Metadata Result: {result:?}");

        let mut response = ServiceDiscoveryResponse::new();
        response.response_message = format!("Add Transport Metadata Result: {result:?}");
        ctx.spawn(sink.success(response).map(|_| ()));
    }

    fn get_service_units_by_type(
        &mut self,
        ctx: RpcContext,
        req: UnitTypeRequest,
        sink: UnarySink<ServiceDiscoveryResponse>,
    ) {
        info!(":::: get_service_units_by_type :::: Received Get Service Units By Type Request");

        let descriptor = <BpServiceDiscoveryAgent as IServiceDiscoveryAgent>::get_descriptor()
            .to_owned()
            + "/default";
        let service_discovery_agent: binder::Strong<dyn IServiceDiscoveryAgent> =
            binder::get_interface(&descriptor).unwrap();

        if req.sdvPackageName.to_string().is_empty()
            || req.serviceBundleName.to_string().is_empty()
            || req.typeName.to_string().is_empty()
        {
            error!(":::: get_service_units_by_type :::: Missing Parameters - sdvPackageName, serviceBundleName and typeName all are required");
            let mut response = ServiceDiscoveryResponse::new();
            response.response_message = "Missing Parameters - sdvPackageName, serviceBundleName and typeName all are required".to_string();
            ctx.spawn(sink.success(response).map(|_| ()));
            return;
        }

        let unit_type = UnitType {
            sdvPackageName: req.sdvPackageName.to_string(),
            serviceBundleName: req.serviceBundleName.to_string(),
            typeName: req.typeName.to_string(),
        };

        let result = service_discovery_agent.listServiceUnitsByType(&unit_type);
        info!(":::: get_service_units_by_type :::: Get Service Units By Type Result: {result:?}");

        let mut response = ServiceDiscoveryResponse::new();
        response.response_message = format!("Get Service Units By Type Result: {result:?}");
        ctx.spawn(sink.success(response).map(|_| ()));
    }

    fn get_service_units_by_name(
        &mut self,
        ctx: RpcContext,
        req: UnitNameRequest,
        sink: UnarySink<ServiceDiscoveryResponse>,
    ) {
        info!(":::: get_service_units_by_name :::: Received Get Service Units By Name Request");

        let descriptor = <BpServiceDiscoveryAgent as IServiceDiscoveryAgent>::get_descriptor()
            .to_owned()
            + "/default";
        let service_discovery_agent: binder::Strong<dyn IServiceDiscoveryAgent> =
            binder::get_interface(&descriptor).unwrap();

        if req.sdvPackageName.to_string().is_empty() || req.serviceBundleName.to_string().is_empty()
        {
            error!(":::: get_service_units_by_name :::: Missing Parameters - Both sdvPackageName and serviceBundleName are required");
            let mut response = ServiceDiscoveryResponse::new();
            response.response_message =
                "Missing Parameters - Both sdvPackageName and serviceBundleName are required"
                    .to_string();
            ctx.spawn(sink.success(response).map(|_| ()));
            return;
        }

        let mut vm_name = None;
        if !req.sdvVmName.to_string().is_empty() {
            vm_name = Some(req.sdvVmName.to_string());
        }

        let mut unit_name = None;
        if !req.unitName.to_string().is_empty() {
            unit_name = Some(req.unitName.to_string());
        }

        let params = UnitNameParameters {
            sdvVmName: vm_name,
            sdvPackageName: req.sdvPackageName.to_string(),
            serviceBundleName: req.serviceBundleName.to_string(),
            unitName: unit_name,
        };

        let result = service_discovery_agent.listServiceUnitsByName(&params);
        info!(":::: get_service_units_by_name :::: Get Service Units By Name Result: {result:?}");

        let mut response = ServiceDiscoveryResponse::new();
        response.response_message = format!("Get Service Units By Name Result: {result:?}");
        ctx.spawn(sink.success(response).map(|_| ()));
    }
}

async fn run() -> anyhow::Result<()> {
    let args: Vec<String> = env::args().collect();
    let port_number = &args[1];
    let server_addr = format!("0.0.0.0:{port_number}");
    let environment = Arc::new(Environment::new(1));
    let service = create_service_discovery(ServiceDiscoveryService::new());
    let mut server = ServerBuilder::new(environment).register_service(service).build()?;
    let port = server.add_listening_port(server_addr, ServerCredentials::insecure())?;
    eprintln!("Service Discovery Test Service listening at port {port}");
    server.start();
    let (tx, rx) = oneshot::channel();
    tokio::spawn(async move {
        eprintln!("Press ENTER to exit...");
        let _ = io::stdin().read(&mut [0]).unwrap();
        tx.send(())
    });
    rx.await?;
    server.shutdown().await?;
    Ok(())
}

fn main() -> anyhow::Result<()> {
    init_logger(LOG_TAG).unwrap();
    info!("Started Test Service For Service Discovery.");
    let rt = tokio::runtime::Builder::new_multi_thread().enable_all().build()?;
    rt.block_on(run())
}
