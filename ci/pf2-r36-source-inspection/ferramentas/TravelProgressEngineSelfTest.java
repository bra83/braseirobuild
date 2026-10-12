package com.braseiro.pathfinder2e;
import org.json.*;
public final class TravelProgressEngineSelfTest {
 private static void require(boolean ok,String m){if(!ok)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  JSONObject first=TravelProgressEngine.advance(10d,0d,4d);
  require(!first.optBoolean("completed"),"trecho não deveria concluir em 4/10 horas");
  require(Math.abs(first.optDouble("completed_hours_after")-4d)<0.01,"progresso parcial incorreto");
  JSONObject second=TravelProgressEngine.advance(10d,first.optDouble("completed_hours_after"),8d);
  require(second.optBoolean("completed"),"trecho deveria concluir");
  require(Math.abs(second.optDouble("unused_hours")-2d)<0.01,"horas excedentes não preservadas");
  boolean rejected=false;try{TravelProgressEngine.advance(0d,0d,2d);}catch(IllegalArgumentException expected){rejected=true;}
  require(rejected,"trecho sem duração não pode teleportar a companhia");
  System.out.println("OK: viagem mantém progresso parcial e só conclui a localização após o tempo total.");
 }
}
