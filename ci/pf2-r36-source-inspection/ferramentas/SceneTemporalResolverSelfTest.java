package com.braseiro.pathfinder2e;

public final class SceneTemporalResolverSelfTest {
    public static void main(String[] args) {
        SceneTemporalResolver.Result morning=SceneTemporalResolver.resolve(
                "Noite",21d,true,"Noite",21d,true,
                "A luz dourada da manhã atravessa a janela.","Luz noturna");
        require("Manhã".equals(morning.label),"A narração da manhã continuou marcada como "+morning.label);
        require(Math.abs(morning.hour-9d)<0.001d,"Hora incorreta: "+morning.hour);
        require("Luz dourada da manhã".equals(morning.lighting),"Iluminação incorreta: "+morning.lighting);

        SceneTemporalResolver.Result memory=SceneTemporalResolver.resolve(
                "Tarde",15d,true,"",0d,false,
                "Ele recorda a noite anterior, mas continua caminhando.","Luz natural da tarde");
        require("Tarde".equals(memory.label),"Uma lembrança alterou indevidamente o relógio para "+memory.label);
        System.out.println("TEMPO DE CENA: manhã canônica e menção histórica aprovadas.");
    }

    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
