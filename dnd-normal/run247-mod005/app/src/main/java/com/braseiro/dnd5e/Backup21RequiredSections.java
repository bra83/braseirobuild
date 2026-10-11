package com.braseiro.dnd5e;

/**
 * Full portable campaign snapshot contract for NEW sealed BACKUP21 archives.
 * Version-21 exports without an integrity seal retain the legacy import path.
 * A present empty array is legitimate; omission or wrong JSON type is not.
 * All named sections below are unconditionally emitted by AppDb.exportAll().
 */
public final class Backup21RequiredSections {
    public interface Presence { boolean object(String key); boolean array(String key); }
    private Backup21RequiredSections() {}
    public static final String[] OBJECTS={
        "campaign","storyState","sceneState","combatState","mapState","partyState",
        "masterRuntimeState","livingWorldState","environmentalDirectorState",
        "travelRulesState","worldSeed","narrativeDiscoveryRuntime"
    };
    public static final String[] ARRAYS={
        "characters","journal","combatants","diceHistory","npcMemories","auditLog",
        "mapCorrections","mapHexNotes","campaignPois","campaignPoiEvents","campaignSettlements",
        "partyActions","partyMemberships","characterControls","companionProposals",
        "knowledgeFacts","quests","factions","worldClocks","worldEvents","pendingTransactions",
        "sceneCommitJournal","worldDomainEvents","pendingActions","effectReceipts",
        "sessionMemories","tacticalTokens","narrativeDiscoveryState",
        "heroLifeProfiles","lifeRelationships","lifeActivities","livingTroupes",
        "masterEpisodes","npcCognitiveStates","guilds","guildRanks","guildMembers",
        "guildContracts","guildProjects","guildSites","guildLedger","guildRelations",
        "emergentArcs","settlementStates","worldHistoryEvents","rumors","rumorKnowledge",
        "campaignLegacies","npcVoiceProfiles","tacticalScenes","localTableSecrets",
        "externalLibrariesExcluded"
    };
    public static String missing(int version,Presence presence) {
        if (version != 21) return "";
        if (presence == null) return "missing_backup_sections_probe";
        for (String key: OBJECTS) if (!presence.object(key)) return "missing_or_invalid_object:" + key;
        for (String key: ARRAYS) if (!presence.array(key)) return "missing_or_invalid_array:" + key;
        return "";
    }
}
