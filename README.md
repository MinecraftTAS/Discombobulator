# Discombobulator
A java preprocessor gradle plugin for Minecraft mod development.

Allows you to develop for multiple Minecraft versions at the same time via statements in the comments

This project seeks to improve the [ReplayMod preprocessor](https://github.com/ReplayMod/preprocessor) by Johni0702 in several ways.

## Basic workflow
When developing, you can add preprocessor statements into your code via comments, which will comment in/out the code depending on the Minecraft version.

A typical project may look like this:
```
workspace
├── 1.19
│   ├── src                      # 1.19 project source
│   └── build.gradle             # 1.19 specific gradle config     
├── MC1.21.11
│   ├── src                      # 1.21.11 project source
│   └── build.gradle             # 1.21.11 specific gradle config
├── src                      # Base project source
├── build.gradle             # Discombobulator gradle config
├── common.gradle            # Common build.gradle for all versions
└── settings.gradle
```
The `Base project source` will contain the unified code for all Minecraft versions and will be committed to version control.

In the `build.gradle` you can add your versions and the order in which they appear:
```gradle
discombobulator {
	versions = [
        "1.21.11":"MC1.21.11"   // If the subfolder is named differently than the version
        "1.19":"",
	]
}
```

When running the `preprocessBase` task, the entire "Base project source" is copied to the subprojects, but with comments enabled/disabled depending on the code.

When coding, run the `preprocessWatch` task, which watches for changes in your code. If a change occurs in a file, that file is then copied back to the base source and also to all other Minecraft versions. It's best to *not* run the task in your IDE but rather in a seperate terminal. Gradle usually locks down the IDE when running a gradle task and stops the IDE from updating the files correctly. A seperate terminal instance fixes that.

If you ever forget to run the watch command, you can use `1.19:preprocessVersion` to preprocess the entire subprojects into all other versions.
## Syntax
### Version blocks
To enable/disable code in certain Minecraft versions, add preprocessor statements like this:
```java
    //# 1.21.11
    // This is the block for MC >= 1.21.11
    category.addDetail(name, callable::call);
    //# 1.19
    // This is the block for MC >= 1.19
    category.setDetail(name, callable::call);
    //# end
```
* The operator used is always the `greater or equal`-operator.
* The order does not matter
* The amount of spaces before and after the `#` does not matter, only a space and a version needs to be in the comment
* It always has to end with a `//# end`
#### Default keyword
Instead of a version, you can add `//# def` and it is basically the `else`-operator. Moreover, `def` will always resolve to the *lowest* version set in the build.gradle, in the example above it's 1.19.

So ideally, our example should look like this:
```java
    //# 1.21.11
    // This is the block for MC >= 1.21.11
    category.addDetail(name, callable::call);
    //# def
    // This is the block for MC >= 1.19
    category.setDetail(name, callable::call);
    //# end
```

This has the advantage, that if you ever want to downgrade your version, like 1.18 or lower, it will immediately work.

#### Nesting
You can have preprocessor statements within preprocessor statements. However, to aid visibility, an additional `#` is added for each nesting level.
```java
    //# 1.9.4
    // Greater or equal than 1.9.4
    public CPacketResourcePackStatus makeStatusPacket(String hash, Action action) {
        //## 1.10.2
        // Greater or equal than 1.10.4
        return new CPacketResourcePackStatus(action);
        //## def
        // Only 1.9.4
        return new CPacketResourcePackStatus(hash, action);
        //## end
    }
    //# def
    // Below 1.9.4
        public C19PacketResourcePackStatus makeStatusPacket(String hash, Action action) {
            return new C19PacketResourcePackStatus(hash, action);
        }
    //# end
```
* A nested `## def` will resolve to the version of the parent block, in this case 1.9.4
* You can add as many nesting levels as you like (or at least until the stack overflows, they are processed recursively)
* A nested block *has to* end with a `## end`
* The order once again does not matter, even in nested blocks

#### Less than
Using a `less than`-operator is not common, but can still be achieved with `greater or equal`-operators
```
    //# 1.21.11
    //# def
    // Enabled in versions below 1.21.11
    category.setDetail(name, callable::call);
    //# end
```

### Patterns
Adding a pattern in an inline comment will make a simple search and replace in that line

```java
System.out.println(mc.window); // @GetWindow;
```
Multiple patterns being applied like so:
```java
Minecraft.getMinecraft().setWindow(mc.window); // @GetWindow,GetMinecraft;
```
and all of them being registered in the build.gradle
```gradle
discombobulator {
	patterns = [
		GetWindow: [
			"def": "mc.window",
			"1.15.2": "mc.getWindow()"
		],
		GetMinecraft: [
			//etc...
		]
	]
}
```
## Config
```gradle
plugins {
	id 'discombobulator' version "${discombobulator_version}"
}

discombobulator {

    // *Required*
    // The versions in descending order. Lowest (e.g. 1.14.4) will be //# def
	versions = [
        "26.1":"",
		"1.21.11":"",
		"1.20.6":"",
		"1.19.4":"",
		"1.18.2":"",
		"1.17.1":"",
		"1.16.5":"",
		"1.16.1":"",
		"1.15.2":"",
		"1.14.4":""
	]

    // Uses the versions above, operates like version blocks with the greater or equal operator
    patterns = [
		GetWindow: [
			"def": "mc.window",
			"1.15.2": "mc.getWindow()"
		],
		GetMinecraft: [
			//etc...
		]
	]

    // If true, inverts the version list, makes the default operator less or equal
    // and sets the default to the highest version.
    // You can technically simply invert the `versions` config above, but ehh...
    inverted = false

    // Overwrites the default line feed to either "\n\r" or "\n"
    // Can also be set by setting the system property "line.separator=\n"
    defaultLineFeed = "\n"
	
    // Excludes file formats from being preprocessed, but still be copied to the folder
    // Uses a org.apache.commons.io.filefilter.WildcardFileFilter to check
    ignoredFileFormats = ["*.png"]
}
```
## Setup
It's recommended to use the [Discombobulator-Template](https://github.com/MinecraftTAS/Discombobulator-Template)