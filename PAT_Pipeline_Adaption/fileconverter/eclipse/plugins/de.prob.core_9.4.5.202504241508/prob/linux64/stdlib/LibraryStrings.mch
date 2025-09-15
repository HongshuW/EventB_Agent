MACHINE LibraryStrings
/* This Library Machine provides a convenient access to the ProB
   external functions for STRING manipulation
   Modified 5/5/2018 to include str2int,... from ClearSy
   Added on 6/1/2019: tolower and toupper and removed ASSERTIONS
   Added on 25/4/2020: strEqualsCaseInsensitive, strIsAlphanumeric
   Added on 26/4/2020: strIsDecimal, int2strWithPad
*/
ABSTRACT_CONSTANTS append, length, split, join, chars, codes,
                   str2int, int2str, int2strWithPad,
                   strIsInt, strIsInStr,
                   int2decStr, decStr2int,
                   hash,
                   tolower, toupper,
                   strEqualsCaseInsensitive,
                   strIsAlphanumeric, strIsDecimal
PROPERTIES
  /* compute the length of a string */
  length: STRING --> INTEGER &
  length = %x.(x:STRING|STRING_LENGTH(x)) &

  /* concatenate two strings; is reversible and can enumerate if only result value known */
  append: STRING * STRING --> STRING &
  append = %(x,y).(x:STRING & y:STRING|STRING_APPEND(x,y)) &

  /* split a string according to a delimiter string into a sequence of strings */
  split: STRING * STRING --> (INTEGER<->STRING) &
  split = %(x,y).(x:STRING & y:STRING|STRING_SPLIT(x,y)) &

  join: (INTEGER<->STRING) * STRING <-> STRING &
  join = %(x,y).(x:(INTEGER<->STRING) & y:STRING|STRING_JOIN(x,y)) &

  /* obtain the characters of a string as a B sequence of strings of length 1 */
  chars: STRING --> (INTEGER <-> STRING) &
  chars = %(s).(s:STRING|STRING_CHARS(s)) &

  /* obtain the characters of a string as a B sequence of Ascii codes; it is reversible */
  codes: STRING --> (INTEGER <-> INTEGER) &
  codes = %(s).(s:STRING|STRING_CODES(s)) &

  /* convert a string to an integer; it is reversible (but not injective) */
  str2int: STRING --> INTEGER &
  str2int = %(s).(s:STRING|STRING_TO_INT(s)) &

  /* convert an integer to a string; it is reversible */
  int2str: INTEGER --> STRING &
  int2str = %(i).(i:INTEGER|INT_TO_STRING(i)) &
  
  /* convert an integer to a string padding with 0s */
  int2strWithPad: INTEGER * INTEGER --> STRING &
  int2strWithPad = %(i,w).(i:INTEGER & w:INTEGER|
                              IF i>=0 THEN STRING_PADLEFT(INT_TO_STRING(i),w,"0")
                              ELSE STRING_APPEND("-",STRING_PADLEFT(INT_TO_STRING(-i),w-1,"0"))
                              END
                            ) &

  /* check if the string represents an integer */
  strIsInt: STRING --> BOOL &
  strIsInt = %(s).(s:STRING|bool(STRING_IS_INT(s))) &

  /* check if the string represents an integer */
  strIsDecimal: STRING --> BOOL &
  strIsDecimal = %(s).(s:STRING|bool(STRING_IS_DECIMAL(s))) &

  /* check whether the second string occurs contiguously within the first string */
  strIsInStr: STRING * STRING --> BOOL &
  strIsInStr = %(x, y).(x:STRING & y:STRING|STRING_CONTAINS_STRING(x,y)) &

  /* convert an integer to a decimal string representation with the precision provided by the second argument */
  int2decStr: INTEGER * INTEGER --> STRING &
  int2decStr = %(i, p).(i:INTEGER & p:INTEGER|INT_TO_DEC_STRING(i,p)) &

  /* convert a decimal string to an integer with the precision provided by the second argument */
  decStr2int: STRING * INTEGER --> INTEGER &
  decStr2int = %(s, p).(s:STRING & p:INTEGER|DEC_STRING_TO_INT(s,p)) &

  /* obtain a hash integer code for a string; uses SICStus Prolog term_hash */
  /* collisions are possible */
  hash: STRING --> INTEGER &
  hash = %(s).(s:STRING|HASH(s)) &

  /* convert strings to lower/upper case and removing diacritical marks (accents, umlaut) */
  tolower: STRING --> STRING &
  tolower = %(s).(s:STRING|STRING_TO_LOWER(s)) &
  
  toupper: STRING --> STRING &
  toupper = %(s).(s:STRING|STRING_TO_UPPER(s))&
  
  /* compare strings ignoring lower/upper case and removing diacritical marks (accents, umlaut) */
  strEqualsCaseInsensitive: STRING * STRING --> BOOL &
  strEqualsCaseInsensitive = 
      %(s1,s2).(s1:STRING & s2:STRING | bool(STRING_EQUAL_CASE_INSENSITIVE(s1,s2))) &

  /* check if a string contains only the characters a-z, A-Z and 0-9 (no diacritical marks allowed) */
  strIsAlphanumeric: STRING --> BOOL &
  strIsAlphanumeric =
      %(s).(s:STRING | bool(STRING_IS_ALPHANUMERIC(s)))
DEFINITIONS
  "LibraryStrings.def";
  "LibraryHash.def"
END

