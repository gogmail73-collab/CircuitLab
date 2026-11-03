package com.circuitlab.app.engine
import com.circuitlab.app.model.*
/** Educational single-loop DC demonstrator; not a general SPICE solver. */
object DcSolver {
 data class Result(val success:Boolean,val message:String,val nodeVoltages:Map<String,Double> = emptyMap(),val branchCurrents:Map<String,Double> = emptyMap())
 fun solve(project:CircuitProject):Result {
  val resistors=project.components.filter{it.type==ComponentType.RESISTOR && it.value>0.0}
  val source=project.components.firstOrNull{it.type==ComponentType.BATTERY && it.enabled}
  if(resistors.isEmpty()||source==null)return Result(false,"أضف بطارية ومقاومة على الأقل.")
  val totalR=resistors.sumOf{it.value}; if(totalR<=0)return Result(false,"قيمة المقاومة غير صالحة.")
  val current=source.value/totalR
  return Result(true,"تيار الحلقة التعليمي = ${"%.4f".format(current)} A",mapOf("GND" to 0.0,"V+" to source.value),resistors.associate{it.label to current})
 }
}