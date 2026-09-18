class BrieySysApb3Timer extends Component{
  val io = new Bundle {
    val apb = slave(Apb3(
      addressWidth = 8,
      dataWidth = 32
    ))
    val interrupt = out Bool()
  }

  val prescaler = Prescaler(16)
  val timerA,timerB = Timer(16)

    // A 64-bit uptime counter that increments every single clock cycle
  val timerValue = Reg(UInt(64 bits)) init(0)
  timerValue := timerValue + 1

  val busCtrl = Apb3SlaveFactory(io.apb)
  val prescalerBridge = prescaler.driveFrom(busCtrl,0x00)

  val timerABridge = timerA.driveFrom(busCtrl,0x40)(
    ticks  = List(True, prescaler.io.overflow),
    clears = List(timerA.io.full)
  )

  val timerBBridge = timerB.driveFrom(busCtrl,0x50)(
    ticks  = List(True, prescaler.io.overflow),
    clears = List(timerB.io.full)
  )
  
  busCtrl.read(timerValue(31 downto 0), 0x60)
  busCtrl.read(timerValue(63 downto 32), 0x64)

  val interruptCtrl = InterruptCtrl(2)
  val interruptCtrlBridge = interruptCtrl.driveFrom(busCtrl,0x10)
  interruptCtrl.io.inputs(0) := timerA.io.full
  interruptCtrl.io.inputs(1) := timerB.io.full
  io.interrupt := interruptCtrl.io.pendings.orR
}
