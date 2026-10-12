package com.braseiro.pathfinder2e;

import org.json.JSONObject;

/** Mantém progresso parcial de um trecho; o grupo só muda de localização ao concluir o tempo calculado. */
public final class TravelProgressEngine {
    private TravelProgressEngine(){}

    public static JSONObject advance(double totalHours,double completedBefore,double availableHours)throws Exception{
        if(totalHours<=0d)throw new IllegalArgumentException("O trecho precisa de duração positiva.");
        double before=clamp(completedBefore,0d,totalHours);double available=Math.max(0d,availableHours);
        double moved=Math.min(available,totalHours-before);double after=before+moved;boolean completed=after>=totalHours-1e-7;
        return new JSONObject().put("total_hours",round(totalHours)).put("completed_hours_before",round(before))
                .put("hours_advanced",round(moved)).put("completed_hours_after",round(after))
                .put("remaining_hours",round(Math.max(0d,totalHours-after))).put("progress_fraction",round(after/totalHours))
                .put("completed",completed).put("unused_hours",round(Math.max(0d,available-moved)));
    }

    private static double clamp(double v,double min,double max){return Math.max(min,Math.min(max,v));}
    private static double round(double v){return Math.round(v*100d)/100d;}
}
