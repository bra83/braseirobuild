package com.braseiro.dnd5e;

/** Explicit concentration event, with externally rolled Constitution save. */
public final class Core2024ConcentrationEvent {
    public enum Type { START, DAMAGE, VOLUNTARY, INCAPACITATED, DEAD, EXPIRED }
    public final String id, caster, spell;
    public final Type type;
    public final long revision;
    public final int damage;
    public final Integer save;
    public Core2024ConcentrationEvent(String id, String caster, Type type,
                                      long revision, String spell, int damage, Integer save) {
        this.id=id; this.caster=caster; this.type=type; this.revision=revision;
        this.spell=spell; this.damage=damage; this.save=save;
    }
}
