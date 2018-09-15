@set DIR_HOME=%~dp0%

java -Dfile.encoding=UTF-8 -classpath "%DIR_HOME%target\classes;%DIR_HOME%lib" teste.Downloader
pause