# TODO

- [x] Add go-to-page controls for EPUB pagination (slider/dialog) and wire them to `gotoEpubPage()` in `ViewerActivity`.
- [x] Persist per-chapter EPUB page positions by extending the Room schema (`Book` entity, DAO, migration) and restoring them on load.
- [x] Update bookmark and highlight navigation to respect per-chapter pagination once persistence is in place.
