MACHINE AssertionsForLibraries
// includes assertions which show how the libraries can be used
SEES LibraryStrings, LibraryMath
DEFINITIONS
  "LibraryStrings.def";
  "LibraryMath.def"
ASSERTIONS

  /* ASSERTIONS for LibraryStrings.mch */
  
  length("abc") = 3;
  length("") = 0;
  length("hello") = 5;
  !(x,y).(x:{"abc","hello",""} & y:{"abc","hello",""}
      => length(append(x,y)) = length(x)+length(y)); /* this did find a counter-example in atom_length of
   SICStus: dif(X,1), atom_length(a,X) finds a solution in SICStus 4.2.0 & 4.2.1*/

  append("abc","abc") = "abcabc";
  append("","abc") = "abc";
  append("abc","") = "abc";
  {x|x:{"abc","abcabc","hello"} & #(prefx).(append(prefx,"c")=x)} = {"abcabc","abc"};
  {x|x/="" & #y.(append(x,y)="abc" & y/="")} = {"a","ab"}; /* compute true prefixes */
  {x|x/="" & #y.(append(y,x)="abc" & y/="")} = {"c","bc"}; /* compute true postfixes */
  {y|y/="" & #(x,z).(append(x,append(y,z))="abc" & length(x)+length(z)>0)} =
    /* compute true substrings */
    {"a","ab","b","bc","c"};
  {y|y/="" & #(x,z).(append(append(x,y),z)="abc" & length(x)+length(z)>0)} =
    /* compute true substrings */
    {"a","ab","b","bc","c"};

  split("filename.ext",".") = ["filename","ext"];
  split("filename.ext","/") = ["filename.ext"];
  split("/usr/local/lib","/") = ["","usr","local","lib"];
  split("/","/") = ["",""];
  split("abcabc","bc") = ["a","a",""];
  split(";",";") = ["",""];
  {x|split(x,";") = ["a","b","c"]} = {"a;b;c"};
  {x|split(x,";") = ["a"]} = {"a"};
  {x|split(x,";") = ["a",""]} = {"a;"};
  {x|split(x,";") = ["",""]} = {";"};
  join(["a","b","c"],";") = "a;b;c";
  !y.(y:{"a;b;", ";", "abc;;;efg", "abc"} => join(split(y,";"),";") = y);
  join(["a_b_c","d"],"_") = "a_b_c_d";
  {r|split(r,"_") = ["a","_","c"]} = {};
  {r|split(r,"_") = ["a","c"]} = {"a_c"};

  chars("") = <>;
  chars("abc") = ["a","b","c"];
  !(x,y).(x:{"abc","hello",""} & y:{"abc","hello",""}
      => chars(append(x,y)) = chars(x)^chars(y));
  /* find strings with b as character: */
  {x|x:{"abc","abcabc","hello"} & "b" : ran(chars(x))} = {"abc", "abcabc"};
  /* now find strings which have the same character set */
  {x|x:{"abc","abcabc","hello"} &
    #y.(y:{"abc","abcabc","hello"} & ran(chars(x))=ran(chars(y)) & x/=y)} = {"abc", "abcabc"};
  /* now find permutations */
  {x|x:{"abc","hello","cba",""} & #(y,p).(y:{"abc","hello","cba",""} & p:perm(1..size(chars(x))) &  x/=y & length(x)=length(y) & /* a bit slower without this condition */
  (p;chars(x)) = chars(y))} = {"abc","cba"};

  ran({a,b,y| chars(y) = [a]^[b] & a:{"a","b"} & b:{"d","e"}}) = {"ad","ae","bd","be"} &
  {x|chars(x) = %i.(i:1..10|"a_")} = {"a_a_a_a_a_a_a_a_a_a_"} &

  card({x,y,z,v| x: {"abc","abcabcd","filename.ext","/usr/local/lib"} &
              y:ran(chars(x)) & z:ran(chars(x)) & y/=z &
              v:ran(chars(x)) & y/=v & z/=v &
              length(x)+length(y)+length(z)+length(v)=6 }) = 6;

  codes("") = <>;
  codes(" ") = [32];
  codes("abc") = [97,98,99];
  !(x,y).(x:{"abc","hello",""} & y:{"abc","hello",""}
      => codes(append(x,y)) = codes(x)^codes(y));
  {x| codes(x) = codes("abc") ^ codes("abc")} = {"abcabc"};
  {x| codes(x) = (codes("abc") ; succ) } = {"bcd"};
  {x| codes(x) = %i.(i:1..26|96+i)} = {"abcdefghijklmnopqrstuvwxyz"};

  INT_TO_STRING(1204) = "1204";
  !x.(x:-4..100 => STRING_TO_INT(INT_TO_STRING(x)) = x);

  str2int("0") = 0;
  str2int("150") = 150;
  str2int("-150") = -150;

  int2str(0) = "0";
  int2str(150) = "150";
  int2str(-150) = "-150";
  
  int2strWithPad(0,0) = "0";
  int2strWithPad(0,1) = "0";
  int2strWithPad(0,2) = "00";
  int2strWithPad(150,2) = "150";
  int2strWithPad(150,3) = "150";
  int2strWithPad(150,4) = "0150";
  int2strWithPad(-150,4) = "-150";
  int2strWithPad(-150,5) = "-0150";
  
  !wid.(wid:{0,1,2,4,8}
           => !x.(x:-4..100 => str2int(int2strWithPad(x,wid)) = x)
        );

  strIsInt("") = FALSE;
  strIsInt("0") = TRUE;
  strIsInt("-0") = TRUE;
  strIsInt("1.4") = FALSE;
  strIsInt("1.0") = FALSE;
  strIsInt("-15") = TRUE;
  strIsInt("a") = FALSE;
  strIsInt("a5b") = FALSE;
  strIsInt("-") = FALSE;

  strIsInStr("b0a","0a") = TRUE;
  strIsInStr("b0a","c") = FALSE;
  strIsInStr("b0a","") = TRUE;
  strIsInStr("","zer") = FALSE;
  strIsInStr("","") = TRUE;

  int2decStr(-156, 0) = "-156";
  int2decStr(769, 2) = "7.69";
  int2decStr(234, -5) = "23400000";
  int2decStr(3, 6) = "0.000003";

  decStr2int("-156", 0) = -156;
  decStr2int("769", 2) = 76900;
  decStr2int("-57.9", -1) = -6;
  decStr2int("44.9", -1) = 4;
  decStr2int("234", -5) = 0;
  decStr2int("3.03", 6) = 3030000;
  
  toupper("aAbB") = "AABB";
  toupper("aàäéèêîçô") = "AAAEEEICO";
  toupper("abcdzABCDZ") = "ABCDZABCDZ";
  toupper("abcdz+äö-ABCDZ") = "ABCDZ+AO-ABCDZ";
  toupper("ÄÖÀÇÎÉ") = "AOACIE";
  tolower("aAbB") = "aabb";
  tolower("abcdzABCDZ") = "abcdzabcdz";
  tolower("abcdz+äö-ABCDZ") = "abcdz+ao-abcdz";
  tolower("ÄÖÀÇÎÉ") = "aoacie";
  strEqualsCaseInsensitive("","") = TRUE;
  strEqualsCaseInsensitive("","a") = FALSE;
  strEqualsCaseInsensitive("a","A") = TRUE;
  strEqualsCaseInsensitive("abcdz+äö-ABCDZ","abcdz+äö-ABCDZ") = TRUE;
  strEqualsCaseInsensitive("abcdz+äö-ABCDZ","ABCDZ+AO-ABCDZ") = TRUE;
  strEqualsCaseInsensitive("abcdz+äö-ABCDZ","abcdz+ao-abcdz") = TRUE;
  strEqualsCaseInsensitive("abcdz+ao-abcdz","ABCDZ+AO-ABCDZ") = TRUE;
  strEqualsCaseInsensitive("aàäéèêîçô","AaAEeEicO") = TRUE;
  strEqualsCaseInsensitive("aàäéèêîçô","aàäéèêîçô") = TRUE;
  strEqualsCaseInsensitive("aàäéèêîçô","aaaeeeico") = TRUE;
  strEqualsCaseInsensitive("a","ö") = FALSE;
  strEqualsCaseInsensitive("ä","ö") = FALSE;
  strEqualsCaseInsensitive("aàäéèê+îçô","aaaeee-ico") = FALSE;
  
  /*
  let ss = %i.(i:1..100| CODES_TO_STRING(%j.(j:1..i|65+((i+j) mod 10))))
  {i,j|i:dom(ss) & j:dom(ss) & strEqualsCaseInsensitive(ss(i),ss(j))=TRUE}
  {i|i:dom(ss) & i mod 2 =0  & strEqualsCaseInsensitive(ss(i),ss(i-1))=TRUE}
  
  */
  
  strIsAlphanumeric("abzAZ019") = TRUE;
  strIsAlphanumeric("1.0") = FALSE;
  strIsAlphanumeric("A") = TRUE;
  strIsAlphanumeric("1") = TRUE;
  strIsAlphanumeric("z") = TRUE;
  strIsAlphanumeric("ä") = FALSE; // Umlaut
  strIsAlphanumeric("_") = FALSE;
  strIsAlphanumeric("abz-AZ019") = FALSE;
  strIsAlphanumeric("") = FALSE;
  
  strIsDecimal("1.0") = TRUE;
  strIsDecimal("0.0") = TRUE;
  strIsDecimal("-0.0") = TRUE;
  strIsDecimal("1") = FALSE;
  strIsDecimal("-1") = FALSE;
  strIsDecimal(".1") = FALSE;
  strIsDecimal("1.") = FALSE;
  strIsDecimal(".") = FALSE;
  strIsDecimal("-") = FALSE;
  strIsDecimal("123456789012345678901234567890.0123456789") = TRUE;
  strIsDecimal("-123456789012345678901234567890.0123456789") = TRUE;
  strIsDecimal("01.10") = TRUE;
  strIsDecimal("a") = FALSE;
  
  STRING_PADLEFT("abc",4,"0") = "0abc";
  STRING_PADLEFT("abc",3,"0") = "abc";
  STRING_PADLEFT("abc",0,"0") = "abc";
  STRING_PADLEFT("abc",5,"-") = "--abc";
  
  STRING_REPLACE("ab%0cd%0","%0","_super_") = "ab_super_cd_super_";
  STRING_REPLACE("ab%0cd%0","%0","") = "abcd";
  STRING_REPLACE("abcd","0","1") = "abcd";
  STRING_REPLACE("","0","1") = "";
  
  /* ASSERTIONS for LibraryMath.mch */
  
  cos(314) = -100;
  sin(314) = 0;
  cos(628) = 100;
  sin(628) = 0;
  !i.(i:0..630 => (sin(i)*sin(i)+cos(i)*cos(i) : (99*99)..(101*101)));
  cos[0..628] = -100..100;
  sin[0..628] = -100..100;
  ([0,157,314];cos) = [100,0,-100];
  !i.(i:0..10000 => sin(i)<=100);
  !i.(i:0..628 => sin(i)=sinx(100,i));
  !i.(i:0..628 => cos(i)=cosx(100,i));
  !i.(i:0..628 => tan(i)=tanx(100,i));
  tan(0) = 0;
  tan(200) = -219; /* tan(2) = -2.185 */
  tanx(1000,2000) = -2185;
  tan(614) = -14; /* tan(6.14) = -0.1441719... */
  tanx(10000,61400) = -1442;
  logx(1,2,1024) = 10;
  logx(100,200,900) = 317; /* log2(9) = 3.16992500144231 */
  logx(10000,20000,90000) = 31699;
  gcd(10,22) = 2;
  gcd(500,30) = 10;
  gcd(-10,-15) = 5;
  msb(1024) = 10;
  msb(1025) = 10;
  abs(10) = 10;
  abs(-10) = 10;
  abs(0) = 0;
  FDIV(-1,4) = -1; // floored division (TLA style) 
  FDIV(3,2) = 1; // this result is like B division
  CDIV(3,2) = 2;
  CDIV(4,2) = 2;
  CDIV(-3,-2) = 2;
  SQRT(100) = 10;
  SQRT(101) = 10;
  /* this works in CLPFD mode: {x|abs(x) = 20000303} = {-20000303, 20000303}; */
  random(1,3) : 1..2;
  random_element(1..2) : 1..2;
  !x.(x:{ {1,2,3}, {4,5,8}, 1..100 } =>  random_element(x) : x);
  random_element({TRUE,FALSE}) : BOOL;
  random_subset(1..2) <: 1..2
/* these cause errors: (and should) 
 ; logx(1,-2,-2) = 1
 ; logx(1,2,0) /= 1
 ; logx(0,2,2) /= 1 
OPERATIONS
  r <-- err1 = BEGIN r:= LOGx(0,2,2) END;
  r <-- err2 = BEGIN r:= SINx(0,2) END
*/

END
