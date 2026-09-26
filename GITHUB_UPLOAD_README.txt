OniGame v0.36.0 - GitHub Web Upload

This build fixes the "pom.xml: No such file or directory" failure.

The new workflow automatically searches for pom.xml up to 4 folders deep.
So it works both when:
- pom.xml is directly in the repository root
- the uploaded files ended up inside one enclosing folder

Recommended upload layout:
repository root/
  .github/
  src/
  pom.xml
  README.md
  ...

Do NOT upload the source ZIP itself as a file and expect Actions to extract it.
Extract the ZIP first, then upload its contents.

Resource pack remains separate:
OniGame-ResourcePack-v37-Synced.zip
