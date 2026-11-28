# BE3 integration policy — provisional, 2026-11-28

This is a BE3 verification baseline, not a final BE1/BE2/FE3 agreement.

## Existing submission boundary
SubmissionService serializes recovery, grading, existing-submission lookup, first-correct
checks, Pet copy/reward application and SubmissionStore.commit under SaveFile.LOCK.
The identity is (userId, sessionId, questionId). Another session creates another record;
prior correct history suppresses its reward. A successful retry returns the saved record
with its original ID, timestamp and earnedXp. It does not mean a new reward was issued.

SubmissionStore validates the copy before writing pending; writes a redo journal, then
pet, then record, and removes pending. Recovery runs before new submissions, validates
both files and refuses conflicting/uncertain state. This is recoverable redo, not a
multi-file atomic transaction or a guarantee against power loss. The application file
lock prevents concurrent app processes; the common Java lock alone does not.

GrowthResult describes a Pet operation, not durable submission success. The integration
uses a committed LearningRecord and a fresh Pet read for UI results. It must not call
PetService.applyCorrectAnswerReward a second time after a SubmissionService success.
No DTO or public BE1/BE2 signature needs to change to match a historical checklist.

## Integer bounds
Keep the current int TXT format. Reject overflowing submissions, never clamp or silently
convert values. Pet is copied before mutation; Store validates nonnegative post-state
and long XP/stat deltas before any pending write. Rejection preserves old files, leaves
no journal and does not advance the session. Zero rewards remain valid at the limit.
No extra overflow guard is added because this boundary already protects persisted data.
BE1 direct Pet/PetService arithmetic remains a separate risk: it uses int addition and
is not covered by Store validation. BE1 must agree on pre-validating BOTH increments
before mutation. This document does not declare that API safe or change it.

GrowthService plans only 0/10 XP and 0/1 stat. Normal quiz sessions contain at most five
questions (at most 50 earned XP). Statistics accumulates int earnedXp in long; even an
int-sized list of int-sized values fits long. LearningRecord permits historical
nonnegative XP beyond 10; do not rewrite those historical values or infer Pet rewards
from imported records. The new large-XP statistics fixture is not a payout scenario.

## Unknown content/category
Preserve current behavior: overall totals include every valid record. Resolve current
and archived IDs through RecordQuestionLookup; never infer a category or map similar
content to a new ID. Conflicting reuse of an ID fails explicitly.
If any category is unresolved, getCategoryStatistics rejects incomplete aggregation
for ALL categories. AppController keeps overall values and records, sends unavailable
category rates and resets the selected category to overall. This intentionally does
not show partial rates as complete rates, zero percent or 'not started'.
Existing UI labels: '분야 확인 불가', '문제 정보 없음', '집계 불가'. Wrong-note uses
'미분류 / 문제 정보 없음'. Proposed explanatory copy for FE3, not a UI change:
'분야를 확인할 수 없는 기록이 있어 분야별 통계를 표시할 수 없습니다. 전체 통계에는 포함됩니다.'
Filtering known records while flagging incomplete rates would require an explicit API/UI
contract; do not silently change the current exception behavior. Deleted content remains
viewable as a record, but this does not authorize it for new/review quiz selection.

## Remaining external decisions
- BE1: application/save boundary and direct reward API overflow behavior.
- BE2: category/ID renaming needs a verified migration map, not guessed remapping.
- FE3/BE2: confirm strict unavailable-category presentation versus partial aggregates.

No Issue checkbox or team agreement is changed by this document. Existing tests cover
concurrency, partial redo, corrupted pending, mixed reward APIs and UI navigation.
IntegrationHardeningTest adds exact XP limit, independent/both limits, zero-reward at
limit, persistent ID/time retry, unchanged existing files and long overall XP with
current/legacy/unknown mixed content. Actual forced process termination is not rerun.

## Independent BE1 probe
A temporary, non-persisting Pet.applyReward(10, INT, 1) probe at Integer.MAX_VALUE
reproduced XP=-2147483639 and INT=-2147483648. This is a direct model API
result, not a claim that the tested BE3 submission path writes corrupted files.
The probe is not retained as a failing regression test and no BE1 source is changed.
