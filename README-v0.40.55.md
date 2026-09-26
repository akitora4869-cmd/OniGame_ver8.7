# OniGame v0.40.55 - Tag Soul Point Economy

- Added match-local Soul Points (SP) for TAG mode.
- Added admin test commands `/og sp add|remove|set|reset|check`; test changes never increment ranking counters.
- Added persistent total-earned and best-single-match SP ranking data.
- Added `/og tag spranking` block registration. Right click opens ranking; comparator toggles total / best-single-match per viewer.
- Added `/og tag spshop` block registration; multiple shops are supported.
- Each shop stores its own mixed skill/item lineup.
- Added `/og spshop add <skill|item> <ID> <price> [uses]` and `/og spshop clear` while looking at a registered shop.
- Default shop includes Sprint, Smoke, Blink, Echo, Heal and Flare.
- TAG heart damage awards 1 SP per configurable 10 effective heart damage.
- Successful Sealing Circle restraint awards configurable 2 SP.
- SP spent in shops does not reduce earned-SP ranking totals.
