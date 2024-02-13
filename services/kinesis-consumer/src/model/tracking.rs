// Automatically generated rust module for 'tracking.proto' file

#![allow(non_snake_case)]
#![allow(non_upper_case_globals)]
#![allow(non_camel_case_types)]
#![allow(unused_imports)]
#![allow(unknown_lints)]
#![allow(clippy::all)]
#![cfg_attr(rustfmt, rustfmt_skip)]


use std::borrow::Cow;
use quick_protobuf::{MessageInfo, MessageRead, MessageWrite, BytesReader, Writer, WriterBackend, Result};
use quick_protobuf::sizeofs::*;
use super::*;

#[allow(clippy::derive_partial_eq_without_eq)]
#[derive(Debug, Default, PartialEq, Clone)]
pub struct TrackingEvent<'a> {
    pub programid: Cow<'a, str>,
    pub checksum: Cow<'a, str>,
    pub customer_id: i32,
    pub user_agent: Cow<'a, str>,
    pub program_name: Cow<'a, str>,
    pub customer_name: Cow<'a, str>,
    pub is_valid: bool,
    pub message_id: Cow<'a, str>,
}

impl<'a> MessageRead<'a> for TrackingEvent<'a> {
    fn from_reader(r: &mut BytesReader, bytes: &'a [u8]) -> Result<Self> {
        let mut msg = Self::default();
        while !r.is_eof() {
            match r.next_tag(bytes) {
                Ok(10) => msg.programid = r.read_string(bytes).map(Cow::Borrowed)?,
                Ok(18) => msg.checksum = r.read_string(bytes).map(Cow::Borrowed)?,
                Ok(24) => msg.customer_id = r.read_int32(bytes)?,
                Ok(34) => msg.user_agent = r.read_string(bytes).map(Cow::Borrowed)?,
                Ok(42) => msg.program_name = r.read_string(bytes).map(Cow::Borrowed)?,
                Ok(50) => msg.customer_name = r.read_string(bytes).map(Cow::Borrowed)?,
                Ok(56) => msg.is_valid = r.read_bool(bytes)?,
                Ok(66) => msg.message_id = r.read_string(bytes).map(Cow::Borrowed)?,
                Ok(t) => { r.read_unknown(bytes, t)?; }
                Err(e) => return Err(e),
            }
        }
        Ok(msg)
    }
}

impl<'a> MessageWrite for TrackingEvent<'a> {
    fn get_size(&self) -> usize {
        0
        + if self.programid == "" { 0 } else { 1 + sizeof_len((&self.programid).len()) }
        + if self.checksum == "" { 0 } else { 1 + sizeof_len((&self.checksum).len()) }
        + if self.customer_id == 0i32 { 0 } else { 1 + sizeof_varint(*(&self.customer_id) as u64) }
        + if self.user_agent == "" { 0 } else { 1 + sizeof_len((&self.user_agent).len()) }
        + if self.program_name == "" { 0 } else { 1 + sizeof_len((&self.program_name).len()) }
        + if self.customer_name == "" { 0 } else { 1 + sizeof_len((&self.customer_name).len()) }
        + if self.is_valid == false { 0 } else { 1 + sizeof_varint(*(&self.is_valid) as u64) }
        + if self.message_id == "" { 0 } else { 1 + sizeof_len((&self.message_id).len()) }
    }

    fn write_message<W: WriterBackend>(&self, w: &mut Writer<W>) -> Result<()> {
        if self.programid != "" { w.write_with_tag(10, |w| w.write_string(&**&self.programid))?; }
        if self.checksum != "" { w.write_with_tag(18, |w| w.write_string(&**&self.checksum))?; }
        if self.customer_id != 0i32 { w.write_with_tag(24, |w| w.write_int32(*&self.customer_id))?; }
        if self.user_agent != "" { w.write_with_tag(34, |w| w.write_string(&**&self.user_agent))?; }
        if self.program_name != "" { w.write_with_tag(42, |w| w.write_string(&**&self.program_name))?; }
        if self.customer_name != "" { w.write_with_tag(50, |w| w.write_string(&**&self.customer_name))?; }
        if self.is_valid != false { w.write_with_tag(56, |w| w.write_bool(*&self.is_valid))?; }
        if self.message_id != "" { w.write_with_tag(66, |w| w.write_string(&**&self.message_id))?; }
        Ok(())
    }
}

