package com.pdc;

public class SemVerUtils {
	public static String combineSemVer(String versionA, String versionB) {
		SemVer semVerA = parseSemVer(versionA);
		SemVer semVerB = parseSemVer(versionB);

		if (semVerA.major == 0 || semVerB.major == 0) {
			int combinedMinor = semVerA.major + semVerB.major + semVerA.minor + semVerB.minor;
			int combinedPatch = semVerA.patch + semVerB.patch;
			return "0." + combinedMinor + "." + combinedPatch;
		} else {
			int combinedMajor = semVerA.major + semVerB.major;
			int combinedMinor = semVerA.minor + semVerB.minor;
			int combinedPatch = semVerA.patch + semVerB.patch;
			return combinedMajor + "." + combinedMinor + "." + combinedPatch;
		}
	}

	private static SemVer parseSemVer(String version) {
		String[] parts = version.split("\\.");
		if (parts.length < 3) {
			throw new IllegalArgumentException("Invalid SemVer string: " + version);
		}
		int major = Integer.parseInt(parts[0]);
		int minor = Integer.parseInt(parts[1]);
		int patch = Integer.parseInt(parts[2]);
		return new SemVer(major, minor, patch);
	}

	private static class SemVer {
		int major;
		int minor;
		int patch;

		public SemVer(int major, int minor, int patch) {
			this.major = major;
			this.minor = minor;
			this.patch = patch;
		}

		public String toString() {
			return this.major + "." + this.minor + "." + this.patch;
		}
	}
}
