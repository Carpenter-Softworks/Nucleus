PROJECT      := nucleus
JAVA_VERSION := 25
MODULES      := core app
FRONTEND_DIR :=
# devkit's scripts read REVISION (default 1.0.0-SNAPSHOT); keep it equal to pom.xml's placeholder.
export REVISION := 0.1.0-SNAPSHOT
DEVKIT := $(shell ./devkitw path)
ifeq ($(DEVKIT),)
$(error devkitw failed; see its message above)
endif
include .devkit/make/common.mk
include .devkit/make/java-maven.mk
