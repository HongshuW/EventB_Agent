MACHINE AssertionsForLibraryHash
// includes assertions which show how the libraries can be used
// it is kept separate from AssertionsForLibraries because the expected hash values are only correct on SICStus and not on e. g. SWI
SEES LibraryStrings
DEFINITIONS
  "LibraryHash.def";
  "LibraryStrings.def"
ASSERTIONS
  hash("") = 231899681;
  hash("a") = 118151707;
  hash("ab c") = 203925323;
  hash("15") = 164651487;
  hash("-44.9") = 268267279
END
