meta:
  id: rtp_midi
  title: rtpmidi
  endian: be
doc: |
  rtpmidi
seq:
  - id: sig
    contents: [0xff, 0xff]
  - id: command
    size: 2
  - id: version
    type: u4
  - id: initiatortoken
    type: u4
  - id: ssrc
    type: u4
  - id: name
    type: str
    encoding: UTF-8
    terminator: 0
