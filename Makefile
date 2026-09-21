JAVAC := javac
JAVA  := java
SRC   := $(wildcard src/passgen/*.java)
OUT   := out

.PHONY: all run jar clean

all: $(OUT)/passgen/Main.class

$(OUT)/passgen/Main.class: $(SRC)
	@mkdir -p $(OUT)
	$(JAVAC) -d $(OUT) $(SRC)

run: all
	$(JAVA) -cp $(OUT) passgen.Main

jar: all
	jar --create --file passgen.jar --main-class passgen.Main -C $(OUT) .

clean:
	rm -rf $(OUT) passgen.jar
