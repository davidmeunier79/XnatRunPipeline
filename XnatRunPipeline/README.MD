**INT - XNAT RUN PIPELINE CLUSTER Plugin**

**Prérequis**

Utilisation de java>=11 : `export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64`

Pour déployer le plugin il faut installer `gradle`

**Déploiement et utilisation du plugin sur XNAT**

1. Allez sur le répertoire **XnatRunPipeline**

   ```
   ~$: cd XnatRunPipeline
   ```
2. Déployer avec gradle

   ```
   ~$: ./gradlew build 
   ```
3. Allez sur le répertoire **XnatRunPipeline/build/libs**

   ```
   ~$: cd XnatRunPipeline/build/libs
   ```
4. Le plugin `xnat-plugin-run-pipeline-data-1.1.1.jar` sera généré
5. Ce plugin doit ensuite être copié dans le dossier des plugins de XNAT (voir la configuration de XNAT, généralement dans `/data/xnat/home/plugins`).

   ```
   ~$: scp xnat-plugin-run-pipeline-data-1.1.1.jar xnat@10.164.0.82:/data/xnat/home/plugins
   ```
6. Redémarrer **tomcat sur xnat**

   ```
   ~$: sudo systemctl restart tomcat8.service
   ```

**Configuration json file xnat**

Pour rajouter une nouvelle image :

Sur xnat allez dans le fichier **/var/lib/tomcat8/xnat\_config\_file\_V1.json**

* la clé : **listPipelines : ajouter le nom de l'image.**
* copier coller une image existant et modifiez la, avec les paramètres de la nouvelle image.