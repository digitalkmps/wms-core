/*
 * Copyright (C) 2009-2020 Lightbend Inc. <https://www.lightbend.com>
 */
package wmscore

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer

// these are needed to serialize Scala style ADT enums with sealed trait and object values with Jackson
// for sending objects between nodes, for the HTTP API See JsonFormats

class DataTypeJsonSerializer extends StdSerializer[WmsStation.DataType](classOf[WmsStation.DataType]) {
  override def serialize(value: WmsStation.DataType, gen: JsonGenerator, provider: SerializerProvider): Unit = {
    val strValue = value match {
      case WmsStation.DataType.Dewpoint    => "d"
      case WmsStation.DataType.Temperature => "t"
      case WmsStation.DataType.Pressure    => "p"
    }
    gen.writeString(strValue)
  }
}

class DataTypeJsonDeserializer extends StdDeserializer[WmsStation.DataType](classOf[WmsStation.DataType]) {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): WmsStation.DataType =
    p.getText match {
      case "d" => WmsStation.DataType.Dewpoint
      case "t" => WmsStation.DataType.Temperature
      case "p" => WmsStation.DataType.Pressure
    }
}

class FunctionJsonSerializer extends StdSerializer[WmsStation.Function](classOf[WmsStation.Function]) {
  override def serialize(value: WmsStation.Function, gen: JsonGenerator, provider: SerializerProvider): Unit = {
    val strValue = value match {
      case WmsStation.Function.Average => "a"
      case WmsStation.Function.Current => "c"
      case WmsStation.Function.HighLow => "h"
    }
    gen.writeString(strValue)
  }
}

class FunctionJsonDeserializer extends StdDeserializer[WmsStation.Function](classOf[WmsStation.Function]) {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): WmsStation.Function =
    p.getText match {
      case "a" => WmsStation.Function.Average
      case "c" => WmsStation.Function.Current
      case "h" => WmsStation.Function.HighLow
    }
}
