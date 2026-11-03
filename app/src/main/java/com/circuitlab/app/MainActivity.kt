package com.circuitlab.app
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.circuitlab.app.engine.DcSolver
import com.circuitlab.app.model.*

private val Bg=Color(0xFF101820); private val Panel=Color(0xFF1A2732); private val Mint=Color(0xFF20D6B0); private val Muted=Color(0xFF91A4B2)
class MainActivity:ComponentActivity(){ override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{CircuitLabApp()}} }
@Composable fun CircuitLabApp(){
 var mode by remember{mutableStateOf("3D Breadboard")}; var project by remember{mutableStateOf(CircuitProject())}; var selected by remember{mutableStateOf<String?>(null)}; var result by remember{mutableStateOf("أضف مكونات لبدء بناء الدائرة.")}; var editTarget by remember{mutableStateOf<CircuitComponent?>(null)}
 MaterialTheme(colorScheme=darkColorScheme(primary=Mint,background=Bg,surface=Panel)){
  Column(Modifier.fillMaxSize().background(Bg).padding(14.dp)){
   Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("CIRCUITLAB",color=Mint,fontSize=22.sp,fontWeight=FontWeight.Black);Text("مختبر الدوائر الإلكترونية",color=Muted,fontSize=12.sp)};Text("ALPHA 0.1",color=Muted,fontSize=10.sp)}
   Spacer(Modifier.height(12.dp))
   Row(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(12.dp)).padding(4.dp)){
    listOf("3D Breadboard","3D Schematic").forEach{m->Box(Modifier.weight(1f).clickable{mode=m}.background(if(mode==m)Mint.copy(alpha=.18f) else Color.Transparent,RoundedCornerShape(9.dp)).padding(10.dp),contentAlignment=Alignment.Center){Text(if(m=="3D Breadboard")"لوحة التجارب" else "المخطط",color=if(mode==m)Mint else Muted)}}
   }
   Spacer(Modifier.height(10.dp));Text("مكتبة العناصر",color=Color.White,fontWeight=FontWeight.Bold)
   LazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp),contentPadding=PaddingValues(vertical=8.dp)){
    items(ComponentType.entries){type->val title=when(type){ComponentType.BATTERY->"بطارية";ComponentType.RESISTOR->"مقاومة";ComponentType.CAPACITOR->"مكثف";ComponentType.INDUCTOR->"ملف";ComponentType.DIODE->"دايود";ComponentType.LED->"LED";ComponentType.SWITCH->"مفتاح";ComponentType.GROUND->"أرضي"}
     Surface(color=Panel,shape=RoundedCornerShape(10.dp),modifier=Modifier.clickable{val(v,u)=when(type){ComponentType.BATTERY->9.0 to "V";ComponentType.RESISTOR->1000.0 to "Ω";ComponentType.CAPACITOR->100.0 to "µF";ComponentType.INDUCTOR->10.0 to "mH";else->0.0 to ""};val n=project.components.size;project=project.copy(components=project.components+CircuitComponent(type=type,label="$title ${n+1}",value=v,unit=u,x=65f+(n%3)*95f,y=75f+(n/3)*70f))}){Text("+ $title",Modifier.padding(12.dp),color=Color.White,fontSize=12.sp)}
    }
   }
   Text(if(mode=="3D Breadboard")"مساحة لوحة التجارب" else "مساحة المخطط",color=Color.White,fontWeight=FontWeight.Bold)
   Box(Modifier.weight(1f).fillMaxWidth().padding(top=7.dp).background(Color(0xFF17232C),RoundedCornerShape(14.dp))){
    Canvas(Modifier.fillMaxSize()){
     val step=24f;for(x in 0..(size.width/step).toInt())for(y in 0..(size.height/step).toInt())drawCircle(Color(0xFF31424D),1.2f,Offset(x*step,y*step))
     if(mode=="3D Breadboard"){drawRoundRect(Color(0xFF244B43),Offset(18f,18f),androidx.compose.ui.geometry.Size(size.width-36f,size.height-36f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(18f));for(r in 0..9)for(c in 0..24)drawCircle(Color(0xFF9BB5A9),2f,Offset(36f+c*18f,50f+r*18f))}
     project.components.forEach{c->val p=Offset(c.x,c.y);val color=if(selected==c.id)Mint else Color.White;drawCircle(color,17f,p,style=Stroke(2.5f));drawLine(color,Offset(p.x-10,p.y),Offset(p.x+10,p.y),3f)}
    }
    Text("${project.components.size} عنصر",Modifier.align(Alignment.TopEnd).padding(9.dp),color=Muted,fontSize=11.sp)
   }
   Spacer(Modifier.height(8.dp));Text("العناصر المضافة",color=Color.White,fontWeight=FontWeight.Bold)
   Column(Modifier.heightIn(max=100.dp)){project.components.takeLast(5).forEach{c->Row(Modifier.fillMaxWidth().clickable{selected=c.id;editTarget=c}.padding(5.dp),verticalAlignment=Alignment.CenterVertically){Text("${c.label} • ${c.value} ${c.unit}",Modifier.weight(1f),color=Color.White,fontSize=12.sp);Text("تعديل",color=Mint,fontSize=11.sp)}}}
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={result=DcSolver.solve(project).message},modifier=Modifier.weight(1f)){Text("▶ محاكاة DC")};OutlinedButton(onClick={project=CircuitProject();result="دائرة جديدة"}){Text("جديد")}}
   Text(result,color=Mint,fontSize=11.sp,modifier=Modifier.padding(top=6.dp))
  }
 }
 editTarget?.let{c->var v by remember(c.id){mutableStateOf(c.value.toString())};AlertDialog(onDismissRequest={editTarget=null},title={Text("تعديل ${c.label}")},text={OutlinedTextField(v,{v=it},label={Text("القيمة ${c.unit}")})},confirmButton={TextButton(onClick={v.toDoubleOrNull()?.let{n->project=project.copy(components=project.components.map{if(it.id==c.id)it.copy(value=n)else it})};editTarget=null}){Text("حفظ")}},dismissButton={TextButton(onClick={editTarget=null}){Text("إلغاء")}},containerColor=Panel)}
}