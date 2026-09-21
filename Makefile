JAVAC := javac
JAVA  := java
SRC   := $(wildcard src/warden/*.java)
OUT   := out

.PHONY: all run jar clean

all: $(OUT)/warden/Main.class

$(OUT)/warden/Main.class: $(SRC)
	@mkdir -p $(OUT)
	$(JAVAC) -d $(OUT) $(SRC)

run: all
	$(JAVA) -cp $(OUT) warden.Main

jar: all
	jar --create --file warden.jar --main-class warden.Main -C $(OUT) .

clean:
	rm -rf $(OUT) warden.jar
