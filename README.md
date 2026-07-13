# cloud-itonami-isco-1323

Open Business Blueprint for **ISCO-08 1323**: Construction Managers — an ISCO
**Wave 1 (design & governance)** occupation per ADR-2607121000. This
is the THIRD wave-1 blueprint batch: management/professional work is
cognitive, **no robotics gate** — eligible for actor implementation
now.

**Maturity: `:implemented`** — ConstructionManagersAdvisor ⊣
ConstructionManagersGovernor as a langgraph StateGraph
(`intake → advise → govern → decide → commit/hold`, human-approval
interrupt), modeled on cloud-itonami-isco-4311's bookkeeping actor.
15 tests / 31 assertions green.

The site HARD invariants — interval and set containment, no partial
credit:

1. **Permit window** — the proposed as-of day must fall inside the
   site's registered permit-issued/expiry window (interval
   containment).
2. **Inspection completeness** — the proposed passed-inspections set
   must be a superset of the site's registered required-inspections
   set. A site is either permitted and inspected, or the advance
   holds.

Also HARD: unregistered/foreign site, unregistered organization,
non-`:propose` effect. Escalations (always human sign-off):
`:issue-occupancy-certificate` (final occupancy sign-off), low
confidence (< 0.6).

AGPL-3.0-or-later, forkable by any qualified operator. Part of the
[cloud-itonami](https://itonami.cloud) open business fleet.
