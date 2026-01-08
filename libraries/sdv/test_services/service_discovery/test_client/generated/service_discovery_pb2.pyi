from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from typing import ClassVar as _ClassVar, Mapping as _Mapping, Optional as _Optional, Union as _Union

DESCRIPTOR: _descriptor.FileDescriptor

class ServiceFqin(_message.Message):
    __slots__ = ("sdvVmName", "sdvPackageName", "serviceBundleName", "serviceInstanceName")
    SDVVMNAME_FIELD_NUMBER: _ClassVar[int]
    SDVPACKAGENAME_FIELD_NUMBER: _ClassVar[int]
    SERVICEBUNDLENAME_FIELD_NUMBER: _ClassVar[int]
    SERVICEINSTANCENAME_FIELD_NUMBER: _ClassVar[int]
    sdvVmName: str
    sdvPackageName: str
    serviceBundleName: str
    serviceInstanceName: str
    def __init__(self, sdvVmName: _Optional[str] = ..., sdvPackageName: _Optional[str] = ..., serviceBundleName: _Optional[str] = ..., serviceInstanceName: _Optional[str] = ...) -> None: ...

class CreateIdentityRequest(_message.Message):
    __slots__ = ("public_key", "service_fqin")
    PUBLIC_KEY_FIELD_NUMBER: _ClassVar[int]
    SERVICE_FQIN_FIELD_NUMBER: _ClassVar[int]
    public_key: str
    service_fqin: ServiceFqin
    def __init__(self, public_key: _Optional[str] = ..., service_fqin: _Optional[_Union[ServiceFqin, _Mapping]] = ...) -> None: ...

class UnitTypeRequest(_message.Message):
    __slots__ = ("sdvPackageName", "serviceBundleName", "typeName")
    SDVPACKAGENAME_FIELD_NUMBER: _ClassVar[int]
    SERVICEBUNDLENAME_FIELD_NUMBER: _ClassVar[int]
    TYPENAME_FIELD_NUMBER: _ClassVar[int]
    sdvPackageName: str
    serviceBundleName: str
    typeName: str
    def __init__(self, sdvPackageName: _Optional[str] = ..., serviceBundleName: _Optional[str] = ..., typeName: _Optional[str] = ...) -> None: ...

class AppMetadata(_message.Message):
    __slots__ = ("version", "valueHolder")
    VERSION_FIELD_NUMBER: _ClassVar[int]
    VALUEHOLDER_FIELD_NUMBER: _ClassVar[int]
    version: int
    valueHolder: str
    def __init__(self, version: _Optional[int] = ..., valueHolder: _Optional[str] = ...) -> None: ...

class RegisterServiceUnitRequest(_message.Message):
    __slots__ = ("unit_type", "app_metadata", "service_unit_name")
    UNIT_TYPE_FIELD_NUMBER: _ClassVar[int]
    APP_METADATA_FIELD_NUMBER: _ClassVar[int]
    SERVICE_UNIT_NAME_FIELD_NUMBER: _ClassVar[int]
    unit_type: UnitTypeRequest
    app_metadata: AppMetadata
    service_unit_name: str
    def __init__(self, unit_type: _Optional[_Union[UnitTypeRequest, _Mapping]] = ..., app_metadata: _Optional[_Union[AppMetadata, _Mapping]] = ..., service_unit_name: _Optional[str] = ...) -> None: ...

class AddTransportMetadataRequest(_message.Message):
    __slots__ = ("value_holder",)
    VALUE_HOLDER_FIELD_NUMBER: _ClassVar[int]
    value_holder: str
    def __init__(self, value_holder: _Optional[str] = ...) -> None: ...

class UnitNameRequest(_message.Message):
    __slots__ = ("sdvVmName", "sdvPackageName", "serviceBundleName", "unitName")
    SDVVMNAME_FIELD_NUMBER: _ClassVar[int]
    SDVPACKAGENAME_FIELD_NUMBER: _ClassVar[int]
    SERVICEBUNDLENAME_FIELD_NUMBER: _ClassVar[int]
    UNITNAME_FIELD_NUMBER: _ClassVar[int]
    sdvVmName: str
    sdvPackageName: str
    serviceBundleName: str
    unitName: str
    def __init__(self, sdvVmName: _Optional[str] = ..., sdvPackageName: _Optional[str] = ..., serviceBundleName: _Optional[str] = ..., unitName: _Optional[str] = ...) -> None: ...

class ServiceDiscoveryResponse(_message.Message):
    __slots__ = ("response_message",)
    RESPONSE_MESSAGE_FIELD_NUMBER: _ClassVar[int]
    response_message: str
    def __init__(self, response_message: _Optional[str] = ...) -> None: ...
