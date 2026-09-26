# OniGame v0.40.56 — TAG dedicated start flow

- `/og tag shrine player`: set shrine player start anchor.
- `/og tag shrine oni`: set delayed Oni appearance point.
- `/og tag start`: start TAG mode. Players begin at the shrine, player bots fill to `tag-mode.player-count`, and Oni appears after `tag-mode.oni-release-seconds`.
- `/og tag forcestart`: test-oriented start; if no human Oni is selected, Oni Bot is used.
- `/og tag stop`: stop TAG match.
- `/og tag status`: show shrine setup and release delay.
- TAG players use max HP 10 and receive no fall damage.
- TAG timer is internal only; player-facing TAG flow does not announce remaining time.
