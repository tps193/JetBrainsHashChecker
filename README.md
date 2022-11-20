The plugin connects to the server run at http://localhost:8095 to compare checksums of locally stored gradle
dependencies with the checksums stored on the server.
Plugin provides following components:
1. Tool Window "Checksum Validation"
2. Action "Analyze Hashes" which is located under Code->Analyze menu or on the tool window toolbar
3. Actions to filter tree results (located on the tool window toolbar)
4. Demo action "Sync Local Checksums" for test purposes. It updates server with local checksums.

Plugin additionally provides following logic:
1. Analyze Hashes action is synchronized with Gradle import project action. When Gradle project is reloaded the checksums
   are revalidated automatically.
2. Tree is expanded automatically, but saves expansion when filters are applied
3. Provides navigation to the component in the build.gradle file
4. Caches server checksums on Application level

To run the plugin please use following command:
gradlew build runIde

For server details please see server repo: 

Plugin limitations:
1. Gradle dependencies which are not actually added to the Module dependencies (for example if newer version is found)
are still shown in the list but with label "No information for artifact found".
2. No proper logging added to the plugin.
3. Actions are not disabled while update or tree recalculation is in progress
4. Actions are not disabled when Gradle sync is in progress
5. Work with tree is not optimized 
6. No shared model module. JSON model classes are copied-pasted between server and plugin 
7. Checksum algorithm is hardcoded as SHA-256 (not compatible with Maven repo now
8. No proper synchronization is provided