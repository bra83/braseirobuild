package br.com.tutorclinico;
import static org.junit.Assert.*;
import java.time.Instant;
import java.time.ZoneId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
public class AgendaReminderReceiverTest {
    private JSONObject appointment(String date,String time,String status)throws Exception{
        return new JSONObject().put("data",date).put("hora",time).put("status",status);
    }
    @Test public void earliestFutureReminderExcludesCancelledPastAndInvalid()throws Exception{
        ZoneId zone=ZoneId.of("America/Sao_Paulo");
        long now=Instant.parse("2026-11-25T12:00:00Z").toEpochMilli();
        JSONArray rows=new JSONArray().put(appointment("2026-11-25","08:00","Agendado"))
            .put(appointment("2026-11-25","09:45","Cancelado")).put(appointment("bad","10:00","Agendado"))
            .put(appointment("2026-11-26","10:00","Agendado")).put(appointment("2026-11-25","10:00","Agendado"));
        assertEquals(Instant.parse("2026-11-25T12:30:00Z").toEpochMilli(),AgendaReminderReceiver.nextReminder(rows,now,zone));
        assertEquals(-1,AgendaReminderReceiver.nextReminder(new JSONArray(),now,zone));
        assertEquals(Instant.parse("2026-11-25T09:30:00Z").toEpochMilli(),AgendaReminderReceiver.nextReminder(rows,Instant.parse("2026-11-25T09:00:00Z").toEpochMilli(),ZoneId.of("UTC")));
    }
}
