package com.braseiro.dnd5e;

/** Single-event mechanical resolution, with no random rolls or side effects. */
public final class Core2024ConcentrationTransitions {
    private Core2024ConcentrationTransitions() {}
    public static final class Change {
        public final String spell,error;
        public final int dc;
        public final boolean passed;
        public final Core2024ConcentrationState.End ended;
        Change(String spell,String error,int dc,boolean passed,Core2024ConcentrationState.End ended) {
            this.spell=spell;this.error=error;this.dc=dc;this.passed=passed;this.ended=ended;
        }
    }
    public static Change resolve(Core2024ConcentrationState s,Core2024ConcentrationEvent e) {
        String spell=s.spellId;
        Core2024ConcentrationState.End end=Core2024ConcentrationState.End.NONE;
        int dc=0;boolean passed=false;
        switch(e.type) {
            case START:
                if(e.spell==null||e.spell.trim().isEmpty()) return new Change(spell,"SPELL_REQUIRED",0,false,end);
                if(s.active()) end=Core2024ConcentrationState.End.REPLACED;
                spell=e.spell;break;
            case DAMAGE:
                if(e.damage<0) return new Change(spell,"NEGATIVE_DAMAGE",0,false,end);
                if(s.active()&&e.damage>0) {
                    dc=Math.max(10,e.damage/2);
                    if(e.save==null) return new Change(spell,"SAVE_REQUIRED",dc,false,end);
                    passed=e.save.intValue()>=dc;
                    if(!passed){spell=null;end=Core2024ConcentrationState.End.FAILED_SAVE;}
                }
                break;
            case VOLUNTARY: case INCAPACITATED: case DEAD: case EXPIRED:
                if(s.active()) {
                    end=e.type==Core2024ConcentrationEvent.Type.VOLUNTARY?
                        Core2024ConcentrationState.End.VOLUNTARY:
                        e.type==Core2024ConcentrationEvent.Type.INCAPACITATED?
                        Core2024ConcentrationState.End.INCAPACITATED:
                        e.type==Core2024ConcentrationEvent.Type.DEAD?
                        Core2024ConcentrationState.End.DEAD:Core2024ConcentrationState.End.EXPIRED;
                    spell=null;
                }
                break;
            default: return new Change(spell,"UNSUPPORTED_EVENT",0,false,end);
        }
        return new Change(spell,null,dc,passed,end);
    }
}
