meta:
  id: timestamp
  title: timestamp
  endian: be
doc: |
  timestamp
seq:
  - id: sig
    contents: [0xff, 0xff]
  - id: command
    contents: ['C', 'K']
  - id: ssrc
    type: u4
  - id: count
    type: u1
  - id: unused
    contents: [0,0,0]
  - id: timestamp1
    type: u8
  - id: timestamp2
    type: u8
  - id: timestamp3
    type: u8