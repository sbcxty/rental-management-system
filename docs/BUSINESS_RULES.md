# Gudo's Nest – Business rules

The rules the app enforces today. When a rule changes, change the code and this file in the same commit,
and add a line to the change log at the end (newest first). Each section names where the rule lives in the
code. **Fixed** means the rule applies to every property and owners can't change it.

## Accounts and portals

Every account is a Renter, an Owner, or both, chosen at sign-up. Code: `AuthService`, `Portal`.

| Rule | Value |
|---|---|
| Sign-up needs | Full name (up to 100 characters), a valid email, a password of at least 8 characters containing a letter, a number and a symbol, and at least one account type |
| Email | One account per email address |
| Suspended accounts | Can't log in |
| Two-role users | Open the Owner portal after login and can switch portals from the sidebar |
| Booking yourself | Not possible: Browse hides your own properties |

## Properties, units and rates

Owners set each unit's nightly rate, cleaning fee and maximum guests; everything else about price is fixed.
Code: `OwnerService`, `UnitDialog`, `MoneyInput`.

| Rule | Value |
|---|---|
| Property types | Whole house (exactly one unit, called "Whole house") or multi-unit (one or more named units) |
| Set by the owner, per unit | Nightly rate (above ₱0.00), cleaning fee (₱0.00 or more, charged once per stay; blank means ₱0.00), maximum guests (at least 1), description |
| Where a house's rates are edited | Add / Edit property, and the pencil on its unit card; a house can't add, duplicate or delete units |
| Money fields | Accept `2800` or `2,800` with up to 2 decimals; no peso sign, letters or negatives |
| Largest amounts | ₱99,999,999.99 for rates, fees and prices; ₱9,999,999,999.99 for a property's acquisition cost |
| Photos | Picked by the owner and copied into the app when the property is saved; the first photo is the cover |
| Unit status | Occupied or unoccupied only, set by the app (see Approval, expiry and check-out) |
| House rules | Owners can't write house rules; renters see only the fixed check-in and check-out times |

## Browsing and booking

A stay is 1 to 30 nights starting tomorrow or later, and the renter pays in full when they book.
Code: `BookingService.checkDates`, `BookingService.book`, `BrowseService`.

| Rule | Value |
|---|---|
| Check-in date | Tomorrow or later |
| Length of stay | At least 1 night, at most 30 nights (**fixed**, `BookingService.MAX_NIGHTS`). Example: check-in Nov 30 allows check-out up to Dec 30 |
| Check-in / check-out times | Check-in from 2:00 PM, check-out by 11:00 AM (**fixed**, all properties) |
| Guests | 1 up to the unit's maximum guests |
| Dates on Browse | Optional, but both or neither; with dates, a property shows only if one of its units fits the guests and is free |
| What blocks dates | Approved stays only. A unit is free again on its check-out day. Pending requests don't block anyone |
| Duplicate requests | A renter can't have two pending requests for the same unit on overlapping dates |
| Payment | Pay first: the booking is saved as pending together with a paid payment, and a receipt is issued |
| Owner's answer | The owner has 48 hours to approve or reject; otherwise the renter is refunded in full (**fixed**) |
| Categories on Browse | All homes (every listing), Whole houses, Multi-unit, Available now (at least one unit has no guest checked in today) |

## Pricing

Total = nightly rate × nights + cleaning fee − long-stay discount. Code: `Price` (Decorator).

| Part | Rule | Set by |
|---|---|---|
| Nights | Unit's nightly rate × number of nights | Owner (rate) |
| Cleaning fee | Added once per stay; no line when it is ₱0.00 | Owner |
| Long-stay discount | 10% off the nights (not the cleaning fee) for 7 nights or more, shown as "Weekly discount (10%)" | **Fixed** (app-wide); owners see a note about it under the rates |

Example: 7 nights at ₱2,800.00 with a ₱500.00 cleaning fee = ₱19,600.00 + ₱500.00 − ₱1,960.00 = ₱18,140.00.
A booking keeps the rate, fee and discount it was made with, so later rate changes don't affect it.

## Approval, expiry and check-out

A booking starts as **Pending** and ends as Completed, Cancelled, Rejected or Expired. Code: `BookingService`,
`BookingRepository.refreshOccupancy`.

| From | Event | To |
|---|---|---|
| Pending | Owner approves within 48 hours | Approved (income recorded; other pending requests that overlap are rejected with a full refund) |
| Pending | Owner rejects | Rejected (full automatic refund) |
| Pending | 48 hours pass, or check-in time arrives first | Expired (full automatic refund) |
| Pending | Renter cancels | Cancelled (full automatic refund) |
| Approved, before check-in | Renter cancels | Cancelled (refund tier, see below) |
| Approved, before check-in | Owner cancels | Cancelled (full automatic refund, income reversed) |
| Approved, after check-in time | Renter confirms check-out, or owner marks it completed | Completed (unit unoccupied again) |

- Statuses are brought up to date when the app opens and whenever a page or booking list loads.
- A unit is **occupied** from the check-in time of an approved booking until that stay is completed.
- Rejected and expired bookings appear under the owner's Cancelled tab.

## Cancellations and refunds

A renter's refund depends on calendar days from today to the check-in date; owner-side cancellations always
refund in full. Code: `RefundPolicy` (Strategy), `BookingService.cancelByRenter`, `OwnerService.approveRefund`.

| Situation | Refund | Review |
|---|---|---|
| Renter cancels an approved booking 7 or more days before check-in | Full | Owner reviews |
| Renter cancels 3 to 6 days before check-in | Total minus a 20% cancellation fee | Owner reviews |
| Renter cancels within 2 days of check-in | None (no refund record is made) | None |
| Renter cancels a pending booking | Full, automatic | None |
| Owner rejects, or doesn't answer within 48 hours | Full, automatic | None |
| Owner cancels an approved booking before check-in | Full, automatic; the income is reversed | None |

- After check-in time, nobody can cancel.
- A renter's cancellation takes effect at once; only the refund waits for review.
- Owner review: Approve processes the refund immediately; Reject means no refund. A refund can be reviewed once.

## Damage reports, finances and messages

| Area | Rule |
|---|---|
| Damage reports | The owner files one against a stay that is under way (staying now or check-out due) or that was completed no more than 7 days after its check-out (`DamageService.REPORT_WITHIN_DAYS`). The File report dialog picks the property first, then the renter, listing only those stays, with a description of up to 1,000 characters, a fee above ₱0.00 and an optional photo (copied to `uploads/damage`). A booking can have more than one report. The renter sees the report on their Damage reports page and pays it (a paid damage fee payment, its receipt and income on the owner's property); the owner can dismiss it while it is unpaid. Each happens once and asks for confirmation. Code: `DamageService` |
| Damage payment deadline | The renter has 48 hours from filing to pay. An unpaid report after that is Overdue and **suspends** the renter: they can still log in, pay, message and see their stays, but can't make new bookings (`BookingService.book` refuses, Browse shows a banner). The suspension isn't stored: it lifts as soon as no report is overdue (paid, even late, or dismissed). Code: `DamageService.checkCanBook` |
| Rental income | Recorded when the owner approves a booking; refunds are recorded as refund transactions |
| Expenses | Added by the owner per property: Maintenance, Supplies or Utilities, an amount above ₱0.00, a date not in the future, a note up to 255 characters. Code: `FinanceService` |
| Conversations | One per renter–owner pair. Can start from a booking or from the Messages search without a booking |
| Messages | Up to 1,000 characters; you can't message yourself |
| Latest booking | The Messages side panel always shows the pair's latest booking |

## Change log

| Date | Decision |
|---|---|
| Nov 15, 2026 | Damage can be reported during a stay or up to 7 days after check-out; File report picks property, then renter |
| Nov 15, 2026 | Renters now see damage reports and pay them (pay only, no dispute; questions go through Messages). Unpaid after 48 hours = can't book until paid or dismissed. The owner no longer charges a report; they can dismiss it |
| Nov 15, 2026 | Damage reports: only stays that have started or finished (now also checked in `OwnerService`), description up to 1,000 characters, optional photo |
| Nov 15, 2026 | Long-stay discount stays app-wide; owners see a note about it under the rates |
| Nov 15, 2026 | The property dialog shows "Check-in and check-out" times, not "House rules" (owners can't write house rules) |
| Nov 15, 2026 | Browse hides a renter's own properties; categories are All homes, Whole houses, Multi-unit, Available now |
| Nov 15, 2026 | Money fields accept thousands commas, no peso sign, and are capped at what the database holds |
| Nov 11, 2026 | Conversations can start without a booking from the Messages search |
| Nov 9, 2026 | Base rules approved: 10% off for 7+ nights, calendar-day refund tiers, owner cancel = full refund, refund review, unit occupied at check-in time, income recorded on approval, two-role users open the Owner portal |
