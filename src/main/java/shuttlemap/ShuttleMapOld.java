package shuttlemap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import org.cadixdev.bombe.type.signature.FieldSignature;
import org.cadixdev.bombe.type.signature.MethodSignature;
import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.lorenz.io.proguard.ProGuardFormat;
import org.cadixdev.lorenz.model.ClassMapping;
import org.cadixdev.lorenz.model.FieldMapping;
import org.cadixdev.lorenz.model.MethodMapping;

import net.fabricmc.mapping.reader.v2.MappingParseException;
import tk.valoeghese.motjin.map.parser.ObfuscationMap;

public class ShuttleMapOld {
	public static void oldMain() throws MappingParseException, IOException {
		MappingSet mojang = new ProGuardFormat().createReader(
				Files.newInputStream(Paths.get("./client.txt")))
				.read().reverse(); // official -> mojang

		ObfuscationMap intermediary = ObfuscationMap.parseTiny("./mappings.tiny");

		writeTiny("shuttle.tiny", intermediary, mojang);
	}

	// from motjin
	private static void writeTiny(String file, ObfuscationMap intermediary, MappingSet mojang) {
		try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
			writer.println("v1\tofficial\tintermediary\tnamed");

			intermediary.forEachObf((obf, intermediaryEntry) -> {
				StringBuilder output = new StringBuilder();
				System.out.println("Mapping\t" + intermediaryEntry.getMappedName());

				// Add mojang name to intermediary class entry
				Optional<? extends ClassMapping<?, ?>> mojmapEntry = mojang.computeClassMapping(obf);

				if (mojmapEntry.isPresent()) {
					intermediaryEntry.setFinalColumnMapping(mojmapEntry.get().getDeobfuscatedName());
				}

				// Add to output tiny
				output.append(intermediaryEntry.toString());

				if (mojmapEntry.isPresent()) {
					ClassMapping<?, ?> mojmapEntry_ = mojmapEntry.get();

					// Process Fields
					intermediaryEntry.fields.forEach(intermediaryFieldEntry -> {
						// Get field entry and set final column mapping
						Optional<? extends FieldMapping> mojmapFieldEntry = mojmapEntry_.computeFieldMapping(FieldSignature.of(intermediaryFieldEntry.obfName, intermediaryFieldEntry.descriptor));

						if (mojmapFieldEntry.isPresent()) {
							intermediaryFieldEntry.setFinalColumnMapping(mojmapFieldEntry.get().getDeobfuscatedName());
						}

						// Add to output
						output.append("\n").append(intermediaryFieldEntry.toString());
					});

					// Process Methods
					intermediaryEntry.methods.forEach(intermediaryMethodEntry -> {
						// Get method entry and set final column mapping
						Optional<? extends MethodMapping> mojmapMethodEntry = mojmapEntry_.getMethodMapping(MethodSignature.of(intermediaryMethodEntry.obfName, intermediaryMethodEntry.signature));

						if (mojmapMethodEntry.isPresent()) {
							if (intermediaryMethodEntry.getMappedName().startsWith("method")) {
								intermediaryMethodEntry.setFinalColumnMapping("mc_" + mojmapMethodEntry.get().getDeobfuscatedName());
							} else {
								intermediaryMethodEntry.setFinalColumnMapping(mojmapMethodEntry.get().getDeobfuscatedName());
							}
						}

						// Add to output
						output.append("\n").append(intermediaryMethodEntry.toString());
					});

					writer.println(output.toString());
				}
			});
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			throw new UncheckedIOException(e);
		}
	}
}
