#!/bin/bash
# Builds build/phosphoros-1.3.3.jar (mod id: lucifertotem) without ForgeGradle:
#  1. compile against MCP-named Minecraft/Forge 1.12.2 jars
#  2. reobfuscate MCP -> SRG with buildtools/Reobf (hierarchy-aware, keyed on owner+name+desc from the SRG jars)
#  3. link-check: common classes against the patched dedicated-server SRG jar, all classes against the patched client
#
# Inputs (not shipped in this repo; they are Mojang/Forge/MCP files you must produce yourself), via environment:
#   MC_DIR      dir with forge-mcp.jar, client-mcp.jar, jsr305-stub.jar, client-patched-srg.jar,
#               server-patched-srg.jar, forge-srg.jar and clientlibs/*.jar (LWJGL etc.)
#   MCP_CSV_DIR dir with MCP fields.csv and methods.csv (mcp_stable-39-1.12)
#   SERVER_LIBS the libraries/ dir of a Forge 14.23.5.2859 server install
#   JAVA_HOME   a JDK 8
set -e; cd "$(dirname "$0")"
: "${MC_DIR:?set MC_DIR}"; : "${MCP_CSV_DIR:?set MCP_CSV_DIR}"; : "${SERVER_LIBS:?set SERVER_LIBS}"
[ -n "$JAVA_HOME" ] && export PATH="$JAVA_HOME/bin:$PATH"
ASM=$(find "$SERVER_LIBS" -name 'asm-debug-all-5.2.jar' | head -1)
LIBS=$(find "$SERVER_LIBS" -name '*.jar' | grep -v forge | tr '\n' ':')
LWJGL=$(ls "$MC_DIR"/clientlibs/*.jar | grep -v 2.9.2 | tr '\n' ':')
rm -rf build && mkdir -p build/mcp build/classes build/tools
javac -source 1.8 -target 1.8 -nowarn -Xlint:-options -cp "$ASM" -d build/tools buildtools/*.java
javac -source 1.8 -target 1.8 -encoding UTF-8 -Xlint:-options -nowarn -cp "$MC_DIR/jsr305-stub.jar:$MC_DIR/forge-mcp.jar:$MC_DIR/client-mcp.jar:$LWJGL:$LIBS" -d build/mcp $(find src/main/java -name '*.java')
java -cp "$ASM:build/tools" Reobf "$MCP_CSV_DIR" build/classes build/mcp "$MC_DIR/client-patched-srg.jar" "$MC_DIR/forge-srg.jar"
echo "== link check: common classes vs dedicated server"; java -cp "$ASM:build/tools" LinkCheck build/classes lucifertotem/client/ "$MC_DIR/server-patched-srg.jar" "$MC_DIR/forge-srg.jar" $(find "$SERVER_LIBS" -name '*.jar' | grep -v forge) | tail -15
echo "== link check: all classes vs client"; java -cp "$ASM:build/tools" LinkCheck build/classes - "$MC_DIR/client-patched-srg.jar" "$MC_DIR/forge-srg.jar" $(ls "$MC_DIR"/clientlibs/*.jar | grep -v 2.9.2) $(find "$SERVER_LIBS" -name '*.jar' | grep -v forge) | tail -15
cp -r src/main/resources/* build/classes/
printf 'Manifest-Version: 1.0\nImplementation-Title: Phosphoros: Purple Mysteries\nImplementation-Version: 1.3.3\nCreated-By: PhosphorosMod\n' > build/MANIFEST.MF
(cd build/classes && find . -exec touch -d '2026-10-19 00:00:00' {} + && jar cfm ../phosphoros-1.3.3.jar ../MANIFEST.MF .)
sha256sum build/phosphoros-1.3.3.jar
