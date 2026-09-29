package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Calendário determinístico para campanhas longas.
 * O padrão Kingmaker usa o calendário comum de Golarion: 12 meses de 30 dias.
 * Configurações já personalizadas continuam respeitadas pelo LivingWorldEngine.
 */
public final class WorldCalendarEngine {
    public static final int GOLARION_DAYS_PER_YEAR = 360;
    private static final String[] MONTHS = {
            "Abadius","Calistril","Pharast","Gozran","Desnus","Sarenith",
            "Erastus","Arodus","Rova","Lamashan","Neth","Kuthona"
    };
    private static final String[] WEEKDAYS = {
            "Moonday","Toilday","Wealday","Oathday","Fireday","Starday","Sunday"
    };
    private WorldCalendarEngine(){}

    public static JSONObject snapshot(int absoluteDay, JSONObject livingState) {
        JSONObject out=new JSONObject();
        try{
            int safe=Math.max(1,absoluteDay);
            int daysPerYear=Math.max(12,livingState==null?GOLARION_DAYS_PER_YEAR:livingState.optInt("days_per_year",GOLARION_DAYS_PER_YEAR));
            int epoch=Math.max(1,livingState==null?1:livingState.optInt("epoch_year",1));
            int year=epoch+(safe-1)/daysPerYear;
            int dayOfYear=((safe-1)%daysPerYear)+1;
            int monthIndex;
            int dayOfMonth;
            if(daysPerYear==GOLARION_DAYS_PER_YEAR){
                monthIndex=Math.min(11,(dayOfYear-1)/30);
                dayOfMonth=((dayOfYear-1)%30)+1;
            }else{
                double scaled=((double)(dayOfYear-1)*12d)/daysPerYear;
                monthIndex=Math.max(0,Math.min(11,(int)Math.floor(scaled)));
                int start=(int)Math.floor((double)monthIndex*daysPerYear/12d)+1;
                dayOfMonth=Math.max(1,dayOfYear-start+1);
            }
            String season=seasonForMonth(monthIndex);
            String weekday=WEEKDAYS[(safe-1)%WEEKDAYS.length];
            out.put("absolute_day",safe).put("year",year).put("day_of_year",dayOfYear)
                    .put("month_index",monthIndex).put("month",MONTHS[monthIndex]).put("day_of_month",dayOfMonth)
                    .put("weekday",weekday).put("season",season)
                    .put("label",dayOfMonth+" de "+MONTHS[monthIndex]+", "+year+" AR • "+weekday+" • "+season)
                    .put("days_per_year",daysPerYear);
        }catch(Exception ignored){}
        return out;
    }

    public static JSONArray milestonesBetween(int beforeDay,int afterDay,JSONObject livingState){
        JSONArray out=new JSONArray();
        if(afterDay<=beforeDay)return out;
        int start=Math.max(1,beforeDay+1),end=Math.max(start,afterDay);
        // Para saltos enormes, iteramos apenas marcos mensais/anuais; no máximo 12 por ano.
        for(int day=start;day<=end;day++){
            JSONObject now=snapshot(day,livingState);
            JSONObject prev=snapshot(Math.max(1,day-1),livingState);
            boolean first=day==1;
            boolean month=first||!now.optString("month").equals(prev.optString("month"));
            boolean season=first||!now.optString("season").equals(prev.optString("season"));
            boolean year=first||now.optInt("year")!=prev.optInt("year");
            if(month||season||year){
                try{
                    JSONObject e=new JSONObject().put("day",day).put("calendar",now);
                    if(month)e.put("month_started",true).put("month_name",now.optString("month"));
                    if(season)e.put("season_started",true).put("season_name",now.optString("season"));
                    if(year)e.put("year_started",true).put("year",now.optInt("year"));
                    out.put(e);
                }catch(Exception ignored){}
            }
        }
        return out;
    }

    public static String seasonForDay(int absoluteDay,JSONObject livingState){
        return snapshot(absoluteDay,livingState).optString("season","Inverno");
    }

    public static double agriculturalMultiplier(String season){
        if("Primavera".equals(season))return 1.08d;
        if("Verão".equals(season))return 1.22d;
        if("Outono".equals(season))return 1.12d;
        return .72d;
    }

    public static double diseaseMultiplier(String season){
        if("Inverno".equals(season))return 1.18d;
        if("Verão".equals(season))return 1.08d;
        return .92d;
    }

    private static String seasonForMonth(int m){
        if(m==11||m<=1)return "Inverno";
        if(m<=4)return "Primavera";
        if(m<=7)return "Verão";
        return "Outono";
    }
}
