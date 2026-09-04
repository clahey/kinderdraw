---
parent: high-level-design
---

# Publishing

## Context and Design Philosophy

This segment covers getting kinderdraw onto the Play Store and keeping it there: signing, release builds, compliance declarations, and the store-listing creative (icon, feature graphic, screenshots, descriptions, slogan). None of it is app behavior — there is nothing here for a user to exercise at runtime — so unlike every other node in this tree, this one owns **no EARS specs and no tests**. The arrow terminates here by design rather than by omission.

It exists as a design doc anyway because it carries real decisions with rationale — why this icon over the alternatives, why this slogan, why the signing key is handled the way it is — which are exactly as easy to lose between sessions as any code decision, and considerably more annoying to reconstruct, since much of the supporting evidence lives in a web console rather than in the repository.

kinderdraw is **child-directed**, which shapes this segment more than any other single fact — see Compliance Posture.

## Path to Publishing

The sequence and what each step is for. `docs/store-listing/release-checklist.md` carries the same sequence as tickable steps with exact commands, and is the one to work from; this section is the reasoning behind it.

Roughly in order:

1. **Local tooling** — the icon and feature graphic are authored as SVG and have to reach Play as PNG, so a rasteriser has to exist. `librsvg2-bin` is the one that matters; ImageMagick alongside it handles the alpha and format constraints the two assets differ on.
2. **Keystore** — add a `kinderdraw` alias to the existing `~/keystores/upload-keystore.jks`, rather than a new file (see Decisions). Never in git. Losing it is mitigated by Play App Signing, which makes the local key a resettable *upload* key.
3. **Target API compliance** — `targetSdk` must meet Play's current minimum. Already satisfied: the HLD fixes `minSdk 30`, `targetSdk`/`compileSdk 36`.
4. **App icon** — adaptive, authored as vector drawables under `androidApp/src/main/res/drawable/` with `mipmap-anydpi-v26` entries. `minSdk 30` clears the API 26 floor for adaptive icons, so no PNG density buckets are needed. The 512×512 store icon is a separate asset — see Store Listing Assets.
5. **App resources** — `res/values/strings.xml` holds `app_name`, and the manifest label resolves through it. Backup and data-extraction rules are declared in `res/xml/`.
6. **Release build type** — `release` in `androidApp/build.gradle.kts`, signed when the credentials are present and unsigned when they aren't (see Signing).
7. **Privacy policy** — **mandatory**, not optional, because the app is child-directed. Written as `PRIVACY.md` at the repo root, to be hosted at its GitHub-rendered URL so no infrastructure is needed. Its claims are grounded in the merged release manifest, which declares no internet permission and no advertising ID.
8. **Store listing** — title, short description, and full description (`docs/store-listing/listing-copy.md`), plus screenshots and the 1024×500 feature graphic.
9. **Content rating** — IARC questionnaire.
10. **Target audience and content** — declare a child age band, which brings the app under Google Play's Families Policy (see Compliance Posture).
11. **Data safety form** — "no data collected, no data shared."
12. **Release build** — `./gradlew bundleRelease` produces the signed AAB, once the signing credentials are in place (see Decisions). Without them the same command still succeeds and yields an *unsigned* bundle, so the signature is worth verifying rather than assuming.
13. **Closed testing** — the track this release targets. Play's production-access gate requires **≥12 testers opted in continuously for ≥14 days** on a *closed* track; internal testing does not count toward it, so an internal run proves the upload pipeline without advancing the clock.

    Two things about this gate are worth knowing before planning around it. **Whether the resulting grant is per developer account or per app is unconfirmed** — it decides whether one qualifying test covers sibling apps or whether each has to serve the window separately, and it is answerable in the Console. And **no prior grant can be assumed**: the sibling Trackr app has not cleared this gate, so whichever app runs the qualifying test is starting from zero. Recruit past twelve, since the count must hold continuously and testers drop out.

The long poles are the store-listing assets (screenshots take real time to produce well) and, uniquely here, the Families Policy surface in steps 7 and 10.

## Compliance Posture

kinderdraw's primary user is a toddler, so it falls under Google Play's Families Policy regardless of how the target-audience question is answered. That is the largest difference between this app's publishing path and a general-audience utility's (see Decisions for why the alternative isn't available).

What follows from it:

- A **privacy policy is required**, not merely advisable.
- **No advertising**, which costs nothing: the HLD's free-and-open-source tenet already rules ads out on every platform, and the Non-Goals list advertising explicitly.
- **SDK restrictions** apply to anything in the ads/analytics space. Also free: the app bundles none.
- The **Advertising ID** must not be used, and the app must not request it.
- Content rating and data-safety answers attract more scrutiny than they would for a general-audience app.

Three artifacts state the same fact about the app in three registers, and Play reads all of them: the privacy policy, the listing copy's no-ads and no-internet claims, and the Data safety declaration. They have to agree, so a change to any one of them is a change to all three.

The app's actual data behavior makes all of these easy to satisfy honestly rather than by argument. It declares no network permission, contains no analytics or ad SDKs, and writes drawings to the device's own shared photo album, where the user's own gallery and backup apps govern them (see the Image Storage LLD's Android Storage Backend).

## Signing

Gradle signs the release bundle. The `release` `signingConfig` in `androidApp/build.gradle.kts` reads `KINDERDRAW_KEYSTORE` and `KINDERDRAW_KEYSTORE_PASSWORD`, which come from `~/.gradle/gradle.properties` — outside the repository, since the committed `gradle.properties` holds ordinary build settings and must never hold these. `KINDERDRAW_KEY_ALIAS` defaults to `kinderdraw`, and the key password falls back to the store password, which is what a PKCS12 keystore requires anyway.

The names avoid dots so each is also expressible as an `ORG_GRADLE_PROJECT_`-prefixed environment variable, which environment variable names could not portably carry otherwise. One build script therefore serves a properties file, an injected CI secret, and an interactive shell without a second code path.

**The config is only created when both properties are present.** Absent, the release build still succeeds and produces an *unsigned* bundle rather than failing. That is deliberate: a checkout without the keystore stays buildable, and a machine that deliberately holds no credentials can still produce an artifact to sign separately. It carries one hazard worth stating plainly — an unsigned bundle is indistinguishable from a signed one until Play rejects it, so the signature is verified with `jarsigner -verify` rather than inferred from a successful build.

Two ways to sign without leaving the password anywhere on the machine, both supported by this same configuration:

- Read it into the environment, unechoed, and let Gradle pick it up: `read -rs` into `ORG_GRADLE_PROJECT_KINDERDRAW_KEYSTORE_PASSWORD`, export, then build. The value never becomes a command argument, so it stays out of both the process table and shell history. A stale daemon may need `--no-daemon`.
- Set nothing, take the unsigned bundle, and run `jarsigner` against it, which prompts on its own terminal.

**Gradle itself cannot prompt for the password**, and this is worth recording because it looks obviously possible and is not. Three separate things block it: `System.console()` is null under Gradle — in the daemon *and* under `--no-daemon` — because Gradle redirects the build's stdin; Gradle forces `java.awt.headless=true`, killing a Swing-dialog fallback, and that is not reliably overridable through `org.gradle.jvmargs`; and `storePassword` is assigned at configuration time, so a prompting `signingConfig` fires on every invocation that configures the release variant rather than only on the one that signs. Opening `/dev/tty` directly, with `stty -echo` around it, does bypass the first two, but only under `--no-daemon` and without addressing the third. The two routes above achieve the same goal without any of it.

## Store Listing Assets

Every color named here comes from the brand palette; `docs/brand.md` is the source of truth for the hex values, and this doc names colors rather than restating them.

**Slogan: "Big fingers welcome."** Used on the feature graphic and available to the listing copy generally.

**App icon.** A single continuous childlike scribble on a brand dark navy field — the mark a toddler actually makes, rather than a tool they'd use. One unbroken line, so it survives any OEM mask and flattens cleanly to the monochrome themed-icon layer. Authored as vector drawables under `androidApp/src/main/res/drawable/`.

- Background layer: brand dark navy, flat.
- Foreground: one stroke, drawn as two overlapping segments so the color changes partway — brand light blue for the first half, brand green for the second. The segments are drawn blue-first so that **green passes over blue where the line crosses itself**, which is what makes the crossing read as one line going over its own path. That is a property of draw order rather than of color choice, so it has to survive any re-authoring; collapsing the two segments into a single path loses it.
- Accent: a brand yellow dot at the stroke's terminus — the point where the line lifts. This is the icon's one branded detail, consistent with the palette's role for yellow as a highlight rather than a fill.
- The crossing only reads with open space around it, which constrains how much else the safe zone can hold.

The adaptive-icon canvas is 108×108 with a 66-unit safe circle; the mark stays inside it.

The Play Console also requires a **512×512 high-res store icon**, a static PNG separate from the adaptive icon. It is composed from the same vector source rather than screenshotted from a launcher, so it stays pixel-exact and re-exportable. Full-bleed square with no rounding or transparency of its own, since Play applies its own masking. The editable master is `docs/store-listing/icon-512.svg`, which carries its own export commands in a comment.

**Feature graphic** (1024×500). Extends the icon's own visual language rather than being a screenshot composite: navy field, the scribble, and the wordmark with the slogan set beneath it. Play does not accept SVG, requires sRGB, and rejects transparency, so the export is a flattened opaque PNG.

## Decisions & Alternatives

| Decision | Chosen | Alternatives Considered | Rationale |
|---|---|---|---|
| Icon concept | A single continuous childlike scribble, two-tone with the second color crossing over the first, yellow dot at the terminus | A crayon alongside the scribble; a scribble that resolves into a letter K; the sun already used by New Picture; three crossed brush strokes; a fingertip with a trailing stroke; a drawing made with the app itself, as Kids Doodle does | The scribble is the product thesis as a shape — the app's promise is that it accepts whatever mark a toddler makes, so the mark *is* the subject. Drawing it with the app was rejected because that trick depends on a distinctive line style, which Kids Doodle has (neon glow) and kinderdraw does not yet; a plain freehand line would say nothing. The crayon variants were rejected on inspection at icon size: a crayon long enough to read as a crayon leaves the safe circle, and shortening it until it fits makes it read as stubby while muddling into the line it sits against. The K was rejected because a toddler cannot draw a K — it is an adult's joke about a child's mark rather than the mark — and it weakens in front of pre-readers. The sun is already New Picture's glyph, so reusing it would give one shape two meanings. Crossed strokes and the fingertip are legible but generic. The crayon directions are worth revisiting if a future brush style gives the line a signature look |
| Display name | `KinderDraw`, MixedCase, used for the launcher label and the Play listing title alike. The repository, package, and `applicationId` stay lowercase | All-lowercase `kinderdraw` as the display name too | MixedCase reads as a deliberate product name in a listing, where all-lowercase reads as a developer's directory name. Identifiers are a separate matter — they are not display names, nobody reads them as the product's name, and changing an `applicationId` after publication is impossible anyway |
| Slogan | "Big fingers welcome." | "Every touch draws."; "Just draw."; "Nothing to get stuck on." | Warm, funny, and aimed squarely at the motor-control thesis that distinguishes the app. "Every touch draws." is a truer statement of the guarantee (a touch that misses a control becomes a stroke rather than nothing) but reads as clipped rather than idiomatic English. The other two are accurate but say less |
| Keystore structure | One keystore *file*, a distinct key alias per app — add a `kinderdraw` alias to the existing `~/keystores/upload-keystore.jks` | A single shared key across all apps; a separate keystore file per app | A signing key can never be rotated away, so fewer files to safeguard is safer; per-app aliases keep a compromise isolated to one app and leave each app independently transferable. Play App Signing lowers the stakes further by making the local key a resettable upload key |
| Release-signing password handling | Gradle signs. A `release` `signingConfig` reads `KINDERDRAW_KEYSTORE` and `KINDERDRAW_KEYSTORE_PASSWORD` from `~/.gradle/gradle.properties`, outside the repository. Absent, the config isn't created and the bundle comes out unsigned rather than the build failing | The password kept only in a person's head, with an interactive `jarsigner` step after an unsigned `bundleRelease`; a 0600 secret file read by that separate signing step; a secret manager, TPM-sealed via `systemd-creds` | Signing in Gradle is the flow the whole toolchain assumes, and `~/.gradle/gradle.properties` is where that flow expects its secret — outside the repo, so the failure mode it was designed against (a password committed to git) can't happen. It also allows releasing from wherever the session is, rather than only from the laptop, which is the point. The head-only alternative forecloses exactly that. The stakes are contained by Play App Signing: the local key is a resettable *upload* key, and it publishes nothing without Console access. A secret manager moves the secret rather than removing it unless hardware-sealed, which is disproportionate here |
| Short description wording | "A drawing app built for toddlers. No ads, no menus, nothing to get stuck on." | Leading with the slogan — "Big fingers welcome. A drawing app made for toddlers, with no ads."; leading with the licence — "Free, open-source drawing for little kids. No ads, no account, no menus." | This is the line shown in search results and browse, so it does more work than the full description and has to say what the app *is* before anything else. The slogan means nothing to someone who doesn't yet know that, which spends the scarcest line in the listing on a phrase that only lands once it's redundant. Licensing leads with something no parent searches for |
| What the listing copy may not claim | No age range in the prose, and no educational or developmental benefit anywhere | Name the 2–4 range in the description; make the developmental claim much of the category makes | There is no evidence behind a developmental claim, and making one invites scrutiny the app has no reason to attract. The age range is real but belongs in Play's target-audience declaration, a structured field with compliance meaning attached — in prose it reads as a promise about outcomes instead of a description of who it's for |
| Promises the copy makes about the future | One: that an account will never be *required* to draw. Everything else — no ads, no purchases, no subscriptions, no network — is stated in the present tense only | Promise the absence of ads permanently, which the free-and-open-source tenet would support; state each of the others as permanent too | A listing claim can't be quietly retracted, and it is read both by people deciding whether to trust the app with a child and by Play alongside the Data safety declaration — so the present tense is what the copy can always stand behind, whatever the app becomes. Even the ads promise is left unmade: the tenet would back it, but saying it buys nothing a plain "there is no advertising" doesn't already say to a parent reading today. The account exception earns its place by being narrow — an optional account stays imaginable, while a *required* one is what a parent is actually checking for, and the HLD's Non-Goals rule it out |
| Store category | Art & Design | Education | The app is a drawing tool, not a teaching one, and makes no educational claim (see the row above). Education would place it against apps promising outcomes this one doesn't. Reversible in the Console at any time, and category mainly drives browse rather than search |
| What the first closed beta contains | Whatever exists when the publishing machinery is ready. Publishing readiness is the critical path; no feature gates the release. A feature that lands before shipping is ready is welcome in it, but nothing waits for one | Hold the beta until the colour picker exists, so the app and its screenshots show more than one control; run an internal-track release first and start the closed track later | Play's production gate needs ≥12 testers opted in for ≥14 continuous days, which is a calendar constraint rather than a quality one — it elapses in the background while development continues, and testers auto-update, so waiting spends the fortnight twice. The behaviour that most needs real toddler testing is the input model, which is already built and depends on no further features. An internal-track release would de-risk the upload mechanics but does not advance the gate, so it buys confidence at the cost of the thing that actually takes time |
| Interactive password entry | Not through Gradle. Either read it unechoed into an `ORG_GRADLE_PROJECT_` environment variable before building, or build unsigned and let `jarsigner` prompt | A prompting `signingConfig`; a `/dev/tty` read with `stty -echo` inside the build script | Gradle cannot prompt — `System.console()` is null, AWT is forced headless, and configuration-time assignment makes the prompt fire on unrelated invocations (see Signing). The `/dev/tty` route defeats the first two but not the third, and only under `--no-daemon`. Both accepted routes reach the same outcome without build-script fragility, and both work unchanged on a machine holding no stored credentials |
| Property names for the signing credentials | `KINDERDRAW_KEYSTORE`, `KINDERDRAW_KEYSTORE_PASSWORD`, `KINDERDRAW_KEY_ALIAS`, `KINDERDRAW_KEY_PASSWORD` — no dots | Dotted names such as `kinderdraw.keystore`, which read more naturally as Gradle properties | Gradle also accepts any property as an `ORG_GRADLE_PROJECT_`-prefixed environment variable, and environment variable names cannot portably contain dots. Undotted names keep the same build script working from a properties file locally and from injected secrets in CI, with no second code path |
| Target audience declaration | Child-directed, under Play's Families Policy | Declare an adult audience to reduce compliance surface, as the sibling Trackr app does | Not available here, and not a close call: the app's entire design is built around toddler motor control, so an adult-audience declaration would be false. The compliance obligations it brings are ones the app already satisfies structurally — no ads, no analytics, no network — rather than ones requiring new work |
| Data safety declaration | No data collected, no data shared | Declare the saved drawings as collected | Under Play's definition, *collect* means transmitting user data off the device to any server. The app declares no network permission and bundles no analytics or ad SDKs, so nothing is transmitted at all. Drawings written to the device's shared photo album are on-device storage, not collection, and any subsequent cloud backup is the user's own gallery/backup app acting on their behalf |
| Advertising ID | Not used | Declare use | No ads, no analytics, no network permission — nothing to read or transmit one. Also independently required by the Families Policy posture |
| Monetization | Free, no ads, no paywall, open source under MIT | Ads; a paid tier; in-app purchases | Direct application of the HLD's free-and-open-source tenet, which names ads as never on the table even where they would be the simpler build. Also the one monetization posture compatible with a child-directed app without substantial extra compliance |
| R8 code shrinking | `isMinifyEnabled = false` | Enable R8 with keep rules and upload the mapping file | With minify off there is nothing to obfuscate, so crash stack traces already carry real names and no mapping file is needed. The source is public anyway, so obfuscation buys nothing. Reversible, and two things would trigger it: app size starting to matter, or Play requiring shrinking. The latter is unverified — the confirmed Play requirement in this area is 16 KB page-size support, which concerns native library alignment rather than minification, and is a separate question to answer on its own terms |

## Open Questions & Future Decisions

### Active

1. **`docs/brand.md` is not present on this branch**, though it exists on `kid-button-press-activation` and lands here when that merges. Every palette reference in this doc, and every brand hex in the icon sources, points at it — so until the merge those hex values have no canonical source in this tree. That doc also keeps a list of every file holding a literal brand hex, which a `docs/brand.md` grep is meant to enumerate; the three icon drawables and `icon-512.svg` belong on it and must be added when the two branches meet.

### Deferred

1. **F-Droid** — the sibling Trackr app publishes to both Play and the official F-Droid catalog, which suits an MIT-licensed app and reaches the de-Googled audience. Not pursued for the first beta. Worth knowing before it is: F-Droid builds from source and signs with its own key, so the same `applicationId` carries different signatures on the two channels, and a user switching sources must uninstall and reinstall.
2. **Amazon Kids store** — a possible later channel. It would shift the icon's audience, putting it in front of pre-readers rather than only parents, which favours one high-contrast recognisable shape over a clever one. The chosen icon already satisfies that, so nothing changes today.
3. **In-app "about" or attribution copy** — no such screen exists yet. When one appears, its positioning text and the store listing's should stay consistent, the way the slogan and short description already are.
4. **Feature graphic composition** — the approach is settled (icon language plus wordmark and slogan) but the layout is not authored. The one structural lesson worth carrying over from the sibling app: keep the type and the artwork in separate bands rather than overlapping them, which reads muddy.

## References

- Root HLD: `docs/high-level-design.md` — Tenets (free and open source, no ads), Non-Goals (advertising on any platform), Key Design Decisions (`minSdk 30`, `targetSdk`/`compileSdk 36` against Play's publishing requirement), Target Users (the toddler audience this segment's compliance posture follows from)
- `docs/brand.md` — the brand palette; canonical source for every hex named here (not yet present on this branch — see Open Questions, Active #1)
- `docs/intent/image-storage/image-storage-design.md § Android Storage Backend` — what the app actually does with saved drawings, which the privacy policy and data-safety declaration are grounded in
- `docs/intent/kid-canvas/widgets/widgets-design.md § Control Catalog` — New Picture's sun glyph, which the icon deliberately does not reuse
- `docs/store-listing/release-checklist.md` — the Path to Publishing above as tickable steps with exact commands; the file to work from
- `docs/store-listing/listing-copy.md` — the Play listing title and descriptions, source of truth
- `docs/store-listing/release-notes.md` — the Play "What's new" text per release, newest first
- `docs/store-listing/icon-512.svg` — editable master for the 512×512 high-res store icon
- `PRIVACY.md` — the privacy policy, whose hosted URL the Play listing points at
