<!--
  Copyright (C) 2026 The Android Open Source Project
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
      http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

# Sound capture testing scenarios

## Abstract

The included scripts offer sound capturing testing in a semi-automated manner,
without depending on actual hardware. \
Those scripts are based on the following [test case "1.7.5 Audio Controls"](https://testtracker.googleplex.com/efforts/testcase/detail/638523900 "1.7.5 Audio Controls").

Warning! \
Depending both on the audio configuration of the QNX hypervisor image and on the
QVM configuration for guest, a “simulator” device must be set. \
Read more instruction about your environment under `Environment preparation`.

## Usage

Two scripts are created for this purpose:

- one for guest to host: `sound_capture_guest_to_host.sh`;
- the other for host to guest: `sound_capture_host_to_guest.sh`.

The required sources files for the playback action are stored in this same
directory.

You need to provide the SSH target for the host and then the ADB address
(usually IP address colon TCP port) as parameters.

The result files should be download automatically into your current working
directory. \
They should be both 16-bit PCM sample — more details down below —, also they
won’t match the source files as we record larger sample than the playback.

## Environment preparation

### QNX hypervisor configuration

Please consult or edit the host file `/etc/system/config/audio/io_audio.conf`,
verify the declaration of a `ctrl` section with name set to `simulator`. \
Then in the list of sound devices under `/dev/snd/`, there should be at least
two files named `pcmCXDYc` and `pcmCXDYp`, while X and Y are numbers and
respectively suffixes c and p are for capture and playback capabilities, and a
related mixer `mixerCXDY`. \
Example on Raspberry Pi image:

```ini
######################################################################
# For the cloud environment we use two audio loopbacks to get full   #
# duplex audio between WebRTC and other io-audio client applications #
######################################################################
[ctrl]
name=simulator
options=in=loopback,rate=48000,voices=2,sample_size=16
input_splitter_enable=1
# Uncomment for LiveAMP integration
#sw_mixer_tap=1
#input_splitter_tap=1
```

Once a simulator is configured, you can verify the mixer capabilities using the
command `mix_ctl -a X:Y groups` (with X and Y the same values than above).

In the output, the name “Simulator In” and “Simulator Out” should be displayed
respectively their “Capture Group” and “Playback Group”. \
Here an example with card 2 (third "card" declared) device 0 on Raspberry Pi 4:

```plaintext
# mix_ctl -a 2:0 groups
Using card 2:0, Mixer Simulator Mixer
"PCM Mixer",0                          - Playback Group
"Simulator In",0                       - Capture Group
"Simulator Out",0                      - Playback Group
```

### QNX guest configuration

In the QNX guest configuration file, the “simulator” device should be declared
in the “vdev virtio-snd” section, both with capture and playback (`stream
capture` and `stream playback` respectively) and its mixer (`control
mixerCXDY`). Assign a different `nid <value>` so the simulator in the guest will
have the name `pcmCX²DY²` with X² the number of the `vdev virtio-snd` section
and Y² the value of the `nid`.

Example of QVM guest configuration:

```plaintext
vdev virtio-snd
    loc 0x20020000
    intr gic:47
    ../..
    # Here add another device, number 2 (see nid value) for the same guest card,
    # providing the host’s "simulator".
    stream playback # To play from guest and record in host
        nid 2
        pcmpath pcmC2D0p
    stream capture # To play from host and record in guest
        nid 2
        pcmpath pcmC2D0c
        formats s16 # Force format, same than main playback
    control mixerC2D0
        ctlpath mixerC2D0
        groupname "PCM Mixer"
        groupcaps volume:mute:gain:balance:fade
```

### SDV Media setup usage

From within the guest VM SDV Media, here an expect output of the `tinypcminfo2`
utility (unprovided by default) of project `tinyalsa`, with `nid 2`: \
Note: you need to compile the utility and push it into your VM (no need to
recreate a full image)

```plaintext
# /data/local/tmp/tinypcminfo2 -D 0 -d 2
Info for card 0, device 2:

PCM out:
      Access:   0x000009
   Format[0]:   0x000004
   Format[1]:   00000000
 Format Name:   S16_LE
   Subformat:   0x000001
        Rate:   min=48000Hz     max=48000Hz
    Channels:   min=2           max=2
 Sample bits:   min=16          max=16
 Period size:   min=480         max=3840
Period count:   min=2           max=16

PCM in:
      Access:   0x000009
   Format[0]:   0x000004
   Format[1]:   00000000
 Format Name:   S16_LE
   Subformat:   0x000001
        Rate:   min=48000Hz     max=48000Hz
    Channels:   min=2           max=2
 Sample bits:   min=16          max=16
 Period size:   min=480         max=3840
Period count:   min=2           max=16
```

As we can notice, the simulator uses 16-bit sample for PCM, so expect your
recorded file to be a 16-bit sample, and mostly to use a 16-bit sample for
playback.
