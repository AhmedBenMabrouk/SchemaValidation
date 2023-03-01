package org.example

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Properties
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer
import org.apache.flink.streaming.util.serialization.JSONKeyValueDeserializationSchema
import org.everit.json.schema.loader.SchemaLoader
import org.everit.json.schema.Schema
import org.json.JSONObject
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.flink.api.common.serialization.{SerializationSchema, SimpleStringSchema}
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.fasterxml.jackson.databind.jsonschema._
import scala.collection.mutable.ListBuffer

object Main {
  def main(args: Array[String]): Unit = {
    
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    // set up Kafka consumer properties
    val consumerProps = new Properties()
    consumerProps.setProperty("bootstrap.servers", "localhost:9092")

    // Create Kafka consumer that reads from topic "raw"
    val kafkaConsumer = new FlinkKafkaConsumer[ObjectNode](
      "raw",
      new JSONKeyValueDeserializationSchema(false),
      consumerProps
    )

    // set up Kafka producer properties
    val producerProps = new Properties()
    producerProps.setProperty("bootstrap.servers", "localhost:9092")

    // create a FlinkKafkaProducer instance to write to topic B
    val myProducer = new FlinkKafkaProducer[ObjectNode](
        "bronze", // target topic
        new SerializationSchema[ObjectNode] {
          override def serialize(element: ObjectNode): Array[Byte] = {
          element.toString.getBytes("UTF8")
          }
        },
        producerProps
    )

     val mapper = new ObjectMapper() with ScalaObjectMapper
    
     val schemaJson = "{\"type\": \"object\", \"properties\": {\"message\": {\"type\": \"string\"}}}"

    val schemaNode = mapper.readTree(schemaJson)
    val schema = schemaNode.get("properties")
    val fieldNames_schema = schema.fieldNames()
    var arr = new ListBuffer[String]()
    while (fieldNames_schema.hasNext) {
    val fieldName = fieldNames_schema.next()
    arr += fieldName
}
    
    
    
    // read from topic A, deserialize to ObjectNode, and write to topic B
    val stream  = env.addSource(kafkaConsumer)
    .filter(msg => {
        val node1 = mapper.readTree(msg.toString)
        val node = node1.get("value")
        val fieldNames2 = node.fieldNames()
        var arr2 = new ListBuffer[String]()
        while (fieldNames2.hasNext) {
        val fieldName = fieldNames2.next()
        arr2 += fieldName
      }
      arr2==arr
      })
    .addSink(myProducer)

    env.execute("Flink Kafka Example")
  }
}
