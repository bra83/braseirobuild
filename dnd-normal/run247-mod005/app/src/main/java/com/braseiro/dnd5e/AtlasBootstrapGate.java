package com.braseiro.dnd5e;

/** Automatic installation is a bounded bootstrap operation, not a retry loop. */
final class AtlasBootstrapGate {
    private boolean attempted;
    private boolean running;
    synchronized boolean tryAutomaticImport(){
        if(attempted||running)return false;
        attempted=true;running=true;return true;
    }
    synchronized void finish(){running=false;}
}
