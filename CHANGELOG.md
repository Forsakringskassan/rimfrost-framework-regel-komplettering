# rimfrost-framework-regel-komplettering changelog

Changelog of rimfrost-framework-regel-komplettering.

## 0.4.1 (2026-11-11)

### Bug Fixes

-  Remove review-findings docs and document identitetsprenumeration and IdentitetUppdateradEvent in README ([a0f13](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/a0f139eec489602) Ulf Slunga)  
-  Use handlaggning.model.Idtyp in RegelKompletteringService ([e0550](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/e05503aed529086) Ulf Slunga)  
-  Pin identitet-prenumeration-adapter to stable 0.0.1 ([21f5a](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/21f5a2c174b6f5f) Ulf Slunga)  
-  Add identity subscription support via identitet-prenumeration ([1c610](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/1c6109f18fe387f) Ulf Slunga)  
-  **deps**  update dependency se.fk.rimfrost.framework.regel.oul:rimfrost-framework-regel-oul to v0.3.1 ([fc848](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/fc8482dbde52d15) renovate[bot])  
-  Handle kafka send faults after rimfrost-framework-regel 1.6.1 change ([e4bad](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/e4bad40b8ee28db) Lars Persson)  

### Dependency updates

- update dependency se.fk.rimfrost.framework.regel:rimfrost-framework-regel to v1.6.1 ([ec57a](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/ec57ab5d6381e0c) renovate[bot])  
- update forsakringskassan/.github digest to cbc0db8 ([6240e](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/6240ec3fe4757c3) renovate[bot])  
## 0.4.0 (2026-11-06)

### Features

-  Remove replyTo and bump dependencies ([78d86](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/78d867432a1cdd1) Lars Persson)  

## 0.3.0 (2026-11-02)

### Features

-  send optional processvyUrl from regelanropet to OUL uppgift (FKPOC-1163) ([54358](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/5435823851139af) LisaWedin_Ductus)  

### Other changes


## 0.2.0 (2026-11-02)

### Features

-  returnera handläggarens identitet i informationsaccess (FKPOC-1103) ([e0d61](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/e0d61e4582a3baa) julolsso)  
-  returnera informationsaccessdata tillsammans med regelns svar i GET-svaret (FKPOC-1103) ([aecad](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/aecad00996cc679) julolsso)  
-  Add access control to provided uppgift endpoints ([e02c2](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/e02c21af42a882a) Lars Persson)  

### Bug Fixes

-  Replace github workflows ([32191](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/32191a58fff3e99) Lars Persson)  
-  returnera HTTP 500 när korrelationsdata saknas och uppdatera kraven (FKPOC-1103) ([2050f](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/2050fa64497da70) julolsso)  
-  Add support for uppgift regelutfall field ([7609e](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/7609ee66af4467c) Lars Persson)  
-  Replace snapshot dependencies in pom.xml ([84e50](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/84e50df16738f7b) Lars Persson)  
-  add SID permission check with PermissionsAdapter and IdentityAdapter ([10a40](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/10a4016f9f535e4) Ulf Slunga)  
-  **deps**  update dependency se.fk.rimfrost.framework.regel.oul:rimfrost-framework-regel-oul-openapi-jaxrs-spec to v0.0.4 ([22982](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/229825782a2951d) renovate[bot])  
-  **deps**  update dependency se.fk.rimfrost.framework.regel.oul:rimfrost-framework-regel-oul-openapi-jaxrs-spec to v0.0.3 ([7bd8c](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/7bd8c1b01111d20) renovate[bot])  
-  **deps**  update dependency se.fk.rimfrost.framework.regel.oul:rimfrost-framework-regel-oul to v0.1.2 ([4a6e7](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/4a6e7cbba32d832) renovate[bot])  
-  **deps**  update dependency se.fk.rimfrost.framework.regel:rimfrost-framework-regel to v1.4.4 ([76f0e](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/76f0ecfa82e673f) renovate[bot])  

### Dependency updates

- update forsakringskassan/.github digest to 625efb1 ([fc65e](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/fc65eddaeecbe5f) renovate[bot])  
- update forsakringskassan/.github digest to cbd74f1 ([5f528](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/5f528bd1d20959e) renovate[bot])  
- update dependency org.apache.maven.plugins:maven-compiler-plugin to v3.16.0 ([ce7b6](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/ce7b6c0977f02d1) renovate[bot])  
- update forsakringskassan/.github digest to bfb01f7 ([b5a0c](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/b5a0c24f749173b) renovate[bot])  
### Other changes


## rimfrost-1_2 (2026-09-18)

## 0.1.1 (2026-09-18)

### Bug Fixes

-  Bump rimfrost-framework-regel-oul version ([53313](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/5331377e20998d8) Lars Persson)  

## 0.1.0 (2026-09-17)

### Features

-  Add maven-jar-plugin to publish test-jar with abstract and test classes ([88078](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/880787984c7175e) Ulf Slunga)  

## 0.0.3 (2026-09-16)

### Bug Fixes

-  Implement OUL openapi interface ([95a9c](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/95a9ca74cebc779) Lars Persson)  
-  Use dedicated conflict faults ([cb2c4](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/cb2c4b6faa1c91d) Lars Persson)  
-  Treat 404 as 200 during OUL end REST operation ([52a4b](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/52a4b9b4b08f3b5) Lars Persson)  

## 0.0.2 (2026-09-15)

### Bug Fixes

-  Remove OUL adapter dependency ([a62f9](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/a62f9224629f06a) Lars Persson)  

### Dependency updates

- pin forsakringskassan/.github action to d1349e6 ([debad](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/debada603813d92) renovate[bot])  
- add renovate.json ([61d29](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/61d29bfd22f76a7) renovate[bot])  
## 0.0.1 (2026-09-11)

### Features

-  Add initial framework implementation ([dc3bc](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/dc3bcb58d65fe50) Lars Persson)  
-  skip Quarkus build for library packaging ([93a71](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/93a7135194cbd51) Ulf Slunga)  
-  require regelimplementations to implement checkKomplettering() ([110ce](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/110cedc8ad7e3eb) Ulf Slunga)  
-  scaffold rimfrost-framework-regel-komplettering with krav ([ea1f9](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/ea1f99653a27944) Ulf Slunga)  

### Bug Fixes

-  Add missing initial structure files ([37294](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/37294a07b8f2079) Lars Persson)  

### Other changes

**Update src/test/java/se/fk/rimfrost/framework/regel/komplettering/RegelKompletteringControllerTest.java**

* Co-authored-by: Ulf Slunga &lt;98820233+UlfSlunga-Sinetiq@users.noreply.github.com&gt; 

[81934](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/819342342727045) larpersso *2026-09-11 11:31:48*

**Create bundle-maven-lib-release.yaml**


[11a2c](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/11a2c3ecd3e7275) Ulf Slunga *2026-09-02 13:37:09*

**Create bundle-maven-lib-ci.yaml**


[1a85e](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/1a85e5b67a1fad4) Ulf Slunga *2026-09-02 13:36:04*

**Create CODEOWNERS**


[4e375](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/4e375799305df2f) Ulf Slunga *2026-09-02 13:35:12*

**Initial commit**


[01447](https://github.com/Forsakringskassan/rimfrost-framework-regel-komplettering/commit/01447d2aaa06e21) larpersso *2026-09-02 13:20:34*


