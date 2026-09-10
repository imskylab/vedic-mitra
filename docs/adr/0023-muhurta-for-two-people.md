# 23. A muhurta chosen for two people takes each factor at its worst

- **Status:** Accepted
- **Date:** 2026-09-10

## Context

Muhurta personalisation shipped for one person: the results screen picks a chart-ready profile,
reduces it to a birth nakshatra and birth Moon sign, and the scorer layers that person's Tarabala and
Chandrabala onto the day's general score. C3 on the roadmap ticks *"personalized muhurta against a
birth Moon"* for it.

**A marriage is not chosen for one person.** Vivah and Vaagdaan are chosen for a couple, and the
model could not express that — so the app either personalised a wedding to the groom alone, or, if
the user picked the bride, to the bride alone. Neither is the question being asked, and nothing on
screen said which one was being answered.

That raises a question the tradition does not settle for us. Tarabala grades a day *for a person*.
Nothing in the sources this project has consulted says how to weigh one person's Sampat tara against
another's Vipat — and a scoring system has to do something.

There is a second, quieter gap. `MuhuratDayViewModel` never injected `ProfileRepository` at all, so
tapping a personalised day opened a screen that had forgotten who it was for.

## Decision

**1. An activity declares how many people it is for.** `MuhurtaActivity.participants` is
`MuhurtaParticipants.ONE` or `COUPLE`; Vaagdaan and Vivah are couples and everything else is not.
`MuhurtaActivityTest` pins that set by name.

The engine says only *how many*. **Roles and gender stay in the feature layer** — the Groom/Bride
framing and the filter behind it need `Gender`, which lives in `:core:datastore`, and
`:core:astronomy` does not depend on it and should not start.

**2. Each factor is taken at its worst across the people.** A day is favourable for a pair only when
it is favourable for each of them. A strong tara for one does not buy off a weak tara for the other.

**This is a stated convention, not a derived result**, and it is stated in those terms to the reader
in the results footer. Per `docs/knowledge-standards.md` this is *rule-transcribed* Compute rather
than the oracle-validated kind: the counting is exact and checkable, the grading is transcribed from
the tradition, and **the combination is neither** — it is a decision this project took.

Its merit is that it is the one combination that asserts nothing extra. Summing the two would encode
a claim that fortunes offset; weighting one side would encode a claim about whose fortune leads. Both
appear in practice; neither is sourced here. Taking the worst says only what the two graded days
already say, and does not average away a warning.

**3. A partly-chosen couple ranks generally.** With only a groom picked, the app does not rank for
him — it ranks generally and says so. Ranking a wedding for half the couple answers a question nobody
asked.

**4. A reason is attributed by id, never by name.** `MuhurtaReason.personId` carries a profile id; the
screen resolves it to a name and formats the sentence from one pattern with indexed arguments. The
engine holds no display copy (ADR 0021), and a name is not an identity — the fault
`docs/knowledge-standards.md` calls *"a claim is not its own key"*.

**5. The day screen reports where the day stands for each person, and changes no window.** The
selection travels on the route as a `people` query argument.

This limit is worth stating plainly, because "personalised muhurta" invites the opposite reading:
**muhurta windows cannot be personalised.** `muhurtasOf(sunTimes, dayOfWeek)` takes a sunrise/sunset
pair and a weekday. Rahu Kalam, Abhijit and Dur Muhurta are facts about a day at a place; they are the
same for everyone alive. What a birth star changes is how the *day* reads, not when its windows fall.
So the standings sit above the windows, not among them.

## Consequences

- Vivah and Vaagdaan now ask for two people, and rank generally until they have both. A user who
  personalised Vivah for one person before will find that selection is no longer offered there.
- **The combination rule needs a citation it does not have.** It is recorded here as this project's
  convention and shown to the reader as one. If a source is found that grades a pair's day directly,
  it supersedes this, and the change is one function.
- Gender-filtered roles exclude a chart-ready profile whose gender is unset or `OTHER`. Matchmaking
  already does this and says nothing; here the count of excluded profiles is carried into the state
  and the screen explains it. **Matchmaking still has the silent version** — worth fixing there.
- A third person cannot be added. Nothing in the model prevents it — the engine takes a list — but no
  activity asks for one, and no rule has been written for what three would mean.

## Alternatives considered

**Sum both people's contributions.** Lets a strong day for one offset a weak day for the other. That
is a real position in practice, but it is a claim about how fortunes interact, and adopting it
silently would be exactly the "unverifiable claim inheriting the credibility of a verified one" that
the knowledge standards exist to prevent.

**Rank generally and annotate per person.** Makes no combined claim at all — the most conservative
option, and it was rejected because selecting people would then change nothing about the order, which
is the entire point of selecting them.

**Persist the selection instead of putting it on the route.** Would make it survive a relaunch. It
would also make the back button lie: returning to a day would show whoever was selected *now* rather
than whoever it was opened for. Remembering a selection is a separate want and can be added without
disturbing this.
