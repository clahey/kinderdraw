# Play Store listing copy

Source of truth for the Play Console listing text. Paste from here rather than
editing in the Console, so the two don't drift.

Positioning follows the Publishing LLD: the differentiator is that the app is
built for toddler motor control, not that it is another drawing app. Every
claim below is one the app actually keeps — see `PRIVACY.md` and the merged
release manifest, which declares no internet permission.

## Title

**KinderDraw** — 10 characters (Play allows 30).

MixedCase is the display form throughout, and matches `@string/app_name`. The
repository, package, and `applicationId` stay lowercase — they aren't display
names.

## Short description

Play allows 80 characters. This is the line shown in search results and
browse, so it does more work than the full description.

> A drawing app built for toddlers. No ads, no menus, nothing to get stuck on.

76 characters.

### Alternatives considered

- *Big fingers welcome. A drawing app made for toddlers, with no ads.* (66) —
  leads with the slogan, but spends the scarce line on a phrase that means
  nothing until you already know what the app is.
- *Free, open-source drawing for little kids. No ads, no account, no menus.*
  (71) — front-loads licensing, which no parent is searching for.

## Full description

> Toddlers can't tap precisely. They can't long-press, they don't understand
> "are you sure?", and they lose interest the moment something goes wrong.
> Most drawing apps are built for adults and then made colourful. KinderDraw
> is built the other way round.
>
> **Big fingers welcome.**
>
> Every control is large and forgiving. Nothing needs a precise tap, nothing
> needs a long press, and a touch that lands slightly off simply draws instead
> of doing nothing. There are no menus to get lost in and no dialogs asking
> questions a two-year-old can't read.
>
> Starting a new picture doesn't ask permission — it quietly saves what's
> there first, so nothing your child made is ever lost to a mis-tap.
>
> Drawings are saved straight into your device's own picture gallery, in their
> own album. They're ordinary pictures: look at them, share them, or delete
> them with the photo app you already use. Nothing is locked inside KinderDraw.
>
> **No ads. Ever.**
>
> There is no advertising in KinderDraw, and there never will be. There are no
> in-app purchases, no subscriptions, no sign-in, and no account. The app
> can't even reach the internet — it doesn't request permission to.
>
> Nothing your child draws leaves your device.
>
> KinderDraw is free and open source under the MIT licence. Anyone can read
> exactly what it does: github.com/clahey/kinderdraw

## Notes for whoever updates this

- Keep "no ads, ever" and the no-internet claim consistent with `PRIVACY.md`
  and with the Data safety declaration. All three have to say the same thing.
- The description deliberately avoids naming an age range in a way that
  reads as a developmental claim. The app is for roughly 2–4 year olds, but
  Play's target-audience declaration is where that belongs, not the marketing
  copy.
- Avoid any claim about educational or developmental benefit. There is no
  evidence for one, and it would invite scrutiny the app has no reason to
  attract.
