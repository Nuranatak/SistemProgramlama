class Configuration
  attr_accessor :fault_tolerance_level, :method

  def initialize(fault_tolerance_level)
    @fault_tolerance_level = fault_tolerance_level
    @method = default_method
  end

  private

  def default_method
    'STRT'
  end
end
