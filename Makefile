PROJECT      := nucleus
JAVA_VERSION := 25
MODULES      := core app
FRONTEND_DIR :=
DEVKIT := $(shell ./devkitw path)
ifeq ($(DEVKIT),)
$(error devkitw failed; see its message above)
endif
include .devkit/make/common.mk
include .devkit/make/java-maven.mk
