# Release checklist

The operational sequence for shipping KinderDraw to Play's closed testing
track. The Publishing LLD carries the *reasoning* behind these steps; this file
carries the doing, with exact commands. When they disagree, the LLD is right
about why and this file is right about how.

`[x]` means done in the repository as it stands. Steps marked **(you)** need a
person: a password, a browser, or a device.

## 1. Local tooling

- [x] **(you)** `sudo apt install librsvg2-bin imagemagick`
- [x] Verify: `rsvg-convert --version` and `magick -version` both answer.

`librsvg2-bin` is the renderer that matters. ImageMagick alone will still
produce a PNG, but it falls back to its own SVG renderer when librsvg is
absent, which distorts round stroke caps and arcs — all this icon is made of.
ImageMagick is needed separately for the alpha and format handling below.

## 2. Repository

- [x] `res/` exists: `strings.xml`, adaptive icon drawables, `mipmap-anydpi-v26`, backup rules
- [x] Manifest label resolves through `@string/app_name` (`KinderDraw`)
- [x] `release` build type, signed when credentials are present — see section 4
- [x] `PRIVACY.md` written
- [x] Contact address in `PRIVACY.md`
- [ ] Merge PR #16 to main, then bring main into this branch — the beta should
      carry the interaction lock and the New Picture fixes, and `docs/brand.md`
      arrives with it
- [ ] Confirm `versionName` / `versionCode` for the first upload
- [ ] **(you)** Push, and take the GitHub URL of `PRIVACY.md` — that URL is what
      Play's listing points at, so it must resolve on a branch that will keep
      existing

## 3. Store assets

Play's requirements, which the verify steps below check against:

| Asset | Size | Format |
|---|---|---|
| Store icon | 512×512 | 32-bit PNG, under 1 MB |
| Feature graphic | 1024×500 | PNG or JPEG, **no alpha channel** |
| Phone screenshots | 2–8 of them | PNG or JPEG, 16:9 or 9:16, 320–3840 px per side |

- [x] Icon master authored: `docs/store-listing/icon-512.svg`
- [x] Export it. `rsvg-convert` alone yields a 24-bit PNG, so the second command
      adds the opaque alpha channel Play's 32-bit spec asks for:

      rsvg-convert -w 512 -h 512 docs/store-listing/icon-512.svg -o docs/store-listing/icon-512.png
      magick docs/store-listing/icon-512.png -alpha set PNG32:docs/store-listing/icon-512.png

- [x] Verify: `magick identify docs/store-listing/icon-512.png` reports
      `512x512` and `TrueColorAlpha`
- [ ] Author `docs/store-listing/feature-graphic.svg` — navy field, the scribble,
      wordmark, and "Big fingers welcome." Keep type and artwork in separate
      bands rather than overlapping
- [ ] Export it:
      `rsvg-convert -w 1024 -h 500 docs/store-listing/feature-graphic.svg -o docs/store-listing/feature-graphic.png`
- [ ] Strip the alpha Play rejects:
      `magick docs/store-listing/feature-graphic.png -alpha remove -alpha off docs/store-listing/feature-graphic.png`
- [ ] Verify: `magick identify -verbose …` shows `1024x500` and no alpha channel
- [ ] **(you)** Capture screenshots on a device or emulator, of whatever the app
      is at that point. Screenshots are cheap to replace on a live listing, so
      they don't wait for a feature — see the Publishing LLD's decision on what
      the first beta contains

## 4. Signing key

Gradle signs the bundle, reading credentials from `~/.gradle/gradle.properties`
— see the Publishing LLD's Decisions for why this over the alternatives.

- [x] `androidApp/build.gradle.kts` has a `release` `signingConfig` that reads
      `KINDERDRAW_KEYSTORE` and `KINDERDRAW_KEYSTORE_PASSWORD`. With neither
      present it isn't created at all, so the build stays green and produces an
      unsigned bundle. **The signed path has not been exercised yet** — nothing
      has ever run with credentials present
- [ ] **(you)** Add a KinderDraw alias to the existing keystore:

      keytool -genkeypair -v \
        -keystore ~/keystores/upload-keystore.jks \
        -alias kinderdraw -keyalg RSA -keysize 2048 -validity 9125

      `-validity 9125` is 25 years. A key that expires is a key the app can
      never be updated with again.

- [ ] Verify it landed: `keytool -list -v -keystore ~/keystores/upload-keystore.jks -alias kinderdraw`
- [ ] **(you)** Put the credentials in `~/.gradle/gradle.properties` — the one
      in this repository is committed and must never hold them:

      KINDERDRAW_KEYSTORE=/home/clahey/keystores/upload-keystore.jks
      KINDERDRAW_KEYSTORE_PASSWORD=…

      `KINDERDRAW_KEY_ALIAS` defaults to `kinderdraw`, and the key password
      falls back to the store password, which is what a PKCS12 keystore
      requires anyway. Don't pass these with `-P`: command-line properties are
      visible in `ps`.

- [ ] Copy the keystore to whichever machines need to sign. One registered
      upload key per app means the same file everywhere, not a second key

## 5. Build and sign

- [ ] `./gradlew :androidApp:bundleRelease` → `androidApp/build/outputs/bundle/release/androidApp-release.aab`, now signed
- [ ] Verify it actually got signed, rather than assuming — the config
      silently skips itself when credentials are missing, so an unsigned
      bundle looks identical until Play rejects it:

      jarsigner -verify -certs androidApp/build/outputs/bundle/release/androidApp-release.aab

- [ ] Get the bundle to whichever machine is doing the upload — `scp`, or ask
      me to hand it over directly

On a machine that deliberately stores no credentials — a laptop where the
password stays in your head — the Publishing LLD's Signing section gives two
ways to sign without them. Gradle cannot prompt for the password; that section
explains why, so nobody rediscovers it a third time.

## 6. Play Console

- [ ] **(you)** Confirm the developer account's status, and whether it is subject
      to the closed-testing gate in section 7
- [ ] **(you)** Create the app. Title `KinderDraw`, category **Art & Design**.
      Note that this permanently reserves `net.clahey.kinderdraw`, and that an
      aborted first upload consumes its versionCode for good — the sibling app
      lost versionCode 1 that way and had to ship as 2
- [ ] **(you)** Paste the listing text from `listing-copy.md` — do not retype it,
      so the two don't drift
- [ ] **(you)** Upload icon, feature graphic, screenshots
- [ ] **(you)** Privacy policy URL, from section 2

Declarations. Every answer below is what the app actually does; the LLD's
Decisions table records why each is the honest answer rather than a chosen one:

- [ ] **(you)** Target audience: child age bands. This puts the app under the
      **Families Policy** — expect the extra questions, and expect them to be
      easy, because the app has no ads, no analytics, and no internet permission
- [ ] **(you)** Content rating: complete the IARC questionnaire
- [ ] **(you)** Data safety: **no data collected, no data shared**
- [ ] **(you)** Ads: **no ads**
- [ ] **(you)** Advertising ID: **not used**
- [ ] **(you)** News app: no. Government app: no. Financial features: none.
      Health features: none
- [ ] **(you)** Upload the signed AAB to the **Closed testing** track
- [ ] **(you)** "What's new" text from `release-notes.md`
- [ ] **(you)** Build the tester list and roll out

Play App Signing enrols automatically on first upload — Google then holds the
real app-signing key and the local alias becomes a resettable *upload* key.

## 7. The production gate

- [ ] **(you)** Confirm in the Console whether the production-access grant is
      per developer account or per app — it decides whether one qualifying test
      covers the sibling apps too
- [ ] **(you)** ≥12 testers opted in, continuously, for ≥14 days on the
      **closed** track. Recruit past twelve; the count has to hold for the whole
      window and people drop out
- [ ] **(you)** Apply for production access once that clears

Internal testing does **not** count toward this. A run on the internal track is
still worth doing first to prove the upload pipeline, but it does not advance
the clock — only the closed track does.

Starting from zero: no sibling app has cleared this gate, so there is no
existing grant to inherit.
