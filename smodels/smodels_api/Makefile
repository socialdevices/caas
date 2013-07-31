CXX = g++
CXXFLAGS = -O3 -W -Wall -fPIC -I../inc/include/ -I../inc/
LFLAGS =  -shared 
SMODELSSRCS_FILE = smodels.cc stack.cc dcl.cc atomrule.cc read.cc \
queue.cc timer.cc list.cc improve.cc program.cc api.cc stable.cc \
tree.cc denant.cc restart.cc
SMODELSSRCS = $(SMODELSSRCS_FILE:%.cc=../src/%.cpp)
SMODELSHEADS_FILE = atomrule.h dcl.h list.h program.h read.h stable.h timer.h \
api.h defines.h print.h queue.h smodels.h stack.h tree.h
SMODELSHEADS = $(SMODELSHEADS_FILE:%.h = ../inc/%.h)
SMODELSOBJS = $(SMODELSSRCS:%.cpp=%.o)

libsmodel.so: engineinterface.o
	$(CXX) $(LFLAGS) engineinterface.o  $(SMODELSOBJS) -o libsmodels.so

%.o: %.cpp
	$(CXX) $(CXXFLAGS) -c $<
