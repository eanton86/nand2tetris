@j
M=0
@8192
D=A
M=D

@KBD
D=M
@RlS
D;JEQ
@j
D=M
@jmax
D=M-D
@LOOP
D;JLE
@SCREEN
A=A+D
M=-1
@j
M=M+1
@LOOP
0;JMP

(RLS)
@j
D=M
@LOOP
D;JEQ

@j
M=M-1
D=M
@SCREEN
A=A+D
M=0

@LOOP
0;JMP
