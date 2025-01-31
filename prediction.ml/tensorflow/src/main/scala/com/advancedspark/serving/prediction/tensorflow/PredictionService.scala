package com.advancedspark.serving.prediction.tensorflow

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.immutable.HashMap

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.JavaConverters.mapAsJavaMapConverter
import scala.util.parsing.json.JSON

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot._
import org.springframework.boot.autoconfigure._
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.context.config.annotation._
import org.springframework.cloud.netflix.eureka.EnableEurekaClient
import org.springframework.cloud.netflix.hystrix.EnableHystrix
import org.springframework.context.annotation._
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation._

@SpringBootApplication
@RestController
@EnableHystrix
class PredictionService {
  val modelRegistry = new scala.collection.mutable.HashMap[String, Array[Byte]]

  @RequestMapping(path=Array("/update-model/{modelName}"),
                  method=Array(RequestMethod.POST),
                  produces=Array("application/json; charset=UTF-8"))
  def updateModel(@PathVariable("modelName") modelName: String, @RequestBody model: Array[Byte]):
      ResponseEntity[HttpStatus] = {

    try {
      // Write the new newModel to local disk
      val path = new java.io.File(s"data/${modelName}/")
      if (!path.isDirectory()) {
        path.mkdirs()
      }

      val file = new java.io.File(s"data/${modelName}/${modelName}.pb")
      if (!file.exists()) {
        file.createNewFile()
      }

      val os = new java.io.FileOutputStream(file)
      os.write(model)

      modelRegistry.put(modelName, model)

      new ResponseEntity(HttpStatus.OK)
    } catch {
       case e: Throwable => {
         throw e
       }
    }
  }

  @RequestMapping(path=Array("/evaluate-model/{modelName}"),
                  method=Array(RequestMethod.POST),
                  produces=Array("application/json; charset=UTF-8"))
  def evaluateModel(@PathVariable("modelName") modelName: String, @RequestBody inputJson: String):
      String = {
    try {
      val inputs = new HashMap[String,Any]()
        //JSON.parseFull(inputJson).get.asInstanceOf[Map[String,Any]]

      val results = new TensorflowCommand("127.0.0.1", 9000, modelName, inputs, "fallback", 25, 20, 10)
        .execute()

      s"""{"results":[${results}]"""
    } catch {
       case e: Throwable => {
         throw e
       }
    }
  }
}

object PredictionServiceMain {
  def main(args: Array[String]): Unit = {
    SpringApplication.run(classOf[PredictionService])
  }
}
