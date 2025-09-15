MACHINE LibraryFiles
/* This Library Machine provides a convenient access to the ProB
   external functions for directory and file manipulation */
ABSTRACT_CONSTANTS files, full_files,
   directories, full_directories,
   current_directory,
   file_exists, file_readable, file_writable, file_executable, file_size, 
   file_created, file_modified, file_accessed,
   directory_exists, directory_readable, directory_writable, directory_searchable,
   directory_created, directory_modified, directory_accessed
PROPERTIES
  /* relative file names of files in a directory */
  files : STRING --> POW(STRING) &
  files = %(x).(x:STRING|FILES(x)) &
  
  /* full file names of files in a directory */
  full_files : STRING --> POW(STRING) &
  full_files = %(x).(x:STRING|FULL_FILES(x)) &
  
  directories : STRING --> POW(STRING) &
  directories = %(x).(x:STRING|DIRECTORIES(x)) &
  
  full_directories : STRING --> POW(STRING) &
  full_directories = %(x).(x:STRING|FULL_DIRECTORIES(x)) &
  
  /* a constant giving the current directory (.) */
  current_directory = "." &
  
  /* checking whether a path points to a directory */
  directory_exists = %x.(x:STRING|bool(DIRECTORY_EXISTS(x))) &
  directory_readable = %x.(x:STRING|bool(DIRECTORY_PROPERTY(x,"readable"))) &
  directory_writable = %x.(x:STRING|bool(DIRECTORY_PROPERTY(x,"writable"))) &
  directory_searchable = %x.(x:STRING|bool(DIRECTORY_PROPERTY(x,"searchable"))) &
  directory_created = %x.(x:STRING|DIRECTORY_PROPERTY_VALUE(x,"create_timestamp")) &
  directory_modified = %x.(x:STRING|DIRECTORY_PROPERTY_VALUE(x,"modify_timestamp")) &
  directory_accessed = %x.(x:STRING|DIRECTORY_PROPERTY_VALUE(x,"access_timestamp")) &
  
  /* checking whether a path points to a file */
  file_exists = %x.(x:STRING|bool(FILE_EXISTS(x))) &
  file_readable = %x.(x:STRING|bool(FILE_PROPERTY(x,"readable"))) &
  file_writable = %x.(x:STRING|bool(FILE_PROPERTY(x,"writable"))) &
  file_executable = %x.(x:STRING|bool(FILE_PROPERTY(x,"executable"))) &
  file_size = %x.(x:STRING|FILE_PROPERTY_VALUE(x,"size_in_bytes")) &
  file_created = %x.(x:STRING|FILE_PROPERTY_VALUE(x,"create_timestamp")) &
  file_modified = %x.(x:STRING|FILE_PROPERTY_VALUE(x,"modify_timestamp")) &
  file_accessed = %x.(x:STRING|FILE_PROPERTY_VALUE(x,"access_timestamp"))
DEFINITIONS
  "LibraryFiles.def"
END

