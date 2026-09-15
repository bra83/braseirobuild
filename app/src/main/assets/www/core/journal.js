export function makeMechanicalJournalEntry({ eventId, revision, kind, summary }) {
  return Object.freeze({ eventId, revision, kind, summary, narrative: false });
}

export function makeNarrativeJournalEntry({ eventId, revision, text }) {
  return Object.freeze({ eventId, revision, kind: 'narrative', text, narrative: true });
}
