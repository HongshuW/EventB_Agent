MACHINE AssertionsForLibraryFiles
// includes assertions which show how the library can be used
// it is kept separate from AssertionsForLibraries because here we need access to the file system
SEES LibraryFiles
DEFINITIONS
  "LibraryFiles.def"
ASSERTIONS
 directory_exists(current_directory)=TRUE;
 file_exists(current_directory)=FALSE;
 !f.(f:files(current_directory) => file_exists(f)=TRUE);
 !f.(f:full_files(current_directory) => file_exists(f)=TRUE);
 !f.(f:files(current_directory) => directory_exists(f)=FALSE);
 !f.(f:full_files(current_directory) => directory_exists(f)=FALSE);
 card(full_files(current_directory)) = card(files(current_directory));
 !d.(d:directories(current_directory) => directory_exists(d)=TRUE);
 !d.(d:full_directories(current_directory) => directory_exists(d)=TRUE);
 !d.(d:directories(current_directory) => file_exists(d)=FALSE);
 !d.(d:full_directories(current_directory) => file_exists(d)=FALSE);
 card(full_directories(current_directory)) = card(directories(current_directory));
 !f.(f:files(current_directory) => file_size(f)>=0);
 !f.(f:files(current_directory) => ((file_readable(f)=TRUE) => file_exists(f)=TRUE)); /* just call file_readable */
 !f.(f:files(current_directory) => ((file_writable(f)=TRUE) => file_exists(f)=TRUE)); /* just call file_readable */
 !f.(f:files(current_directory) => ((file_executable(f)=TRUE) => file_exists(f)=TRUE)); /* just call file_executable */
 /* !f.(f:files(current_directory) => (file_created(f)<=file_modified(f))); this does not seem to hold !! */
 /* !f.(f:files(current_directory) => (file_modified(f)<=file_accessed(f))); this does not seem to hold !! */
 !f.(f:files(current_directory) => (file_modified(f)>=0 & file_created(f)>=0 & file_accessed(f)>=0));
 !d.(d:directories(current_directory) => (directory_readable(d)=TRUE => directory_exists(d)=TRUE));
 !d.(d:directories(current_directory) => (directory_writable(d)=TRUE => directory_exists(d)=TRUE));
 !d.(d:directories(current_directory) => (directory_searchable(d)=TRUE => directory_exists(d)=TRUE));
 /* !d.(d:directories(current_directory) => (directory_created(d)<=directory_modified(d))); this does not seem to hold !! */
 /* !d.(d:directories(current_directory) => (directory_modified(d)<=directory_accessed(d)))this does not seem to hold !! */
 !d.(d:directories(current_directory) => (directory_modified(d)>=0 & directory_created(d)>=0 & directory_accessed(d)>=0))
/*
 Examples:
 {x,y|x:full_files(".") & y=file_size(x) & y>1024*1024}
*/
END
